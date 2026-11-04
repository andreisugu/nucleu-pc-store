import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Product = {
  id: number
  name: string
  description: string
  specifications: string
  price: number
  stock: number
  supplier: string
  deliveryMethod: string
  category: string
}

type Category = { id: number; name: string }
type LoadState = 'loading' | 'ready' | 'error'
type AuthMode = 'login' | 'register'
type Session = { accessToken: string; email: string; role: 'CLIENT' | 'ADMIN' }
type ProductDraft = {
  name: string
  description: string
  specifications: string
  price: string
  stock: string
  supplier: string
  deliveryMethod: string
  category: string
}

const emptyProduct: ProductDraft = {
  name: '', description: '', specifications: '', price: '', stock: '',
  supplier: '', deliveryMethod: '', category: '',
}

async function readJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`Cererea a eșuat (${response.status})`)
  return response.json() as Promise<T>
}

async function requestJson<T>(url: string, token: string, method: string, body?: unknown): Promise<T> {
  const response = await fetch(url, {
    method,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as { detail?: string; message?: string }
    throw new Error(error.detail ?? error.message ?? `Cererea a eșuat (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function formatPrice(price: number) {
  return new Intl.NumberFormat('ro-RO', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price)
}

function ProductArtwork({ index }: { index: number }) {
  return (
    <div className={`product-art product-art--${index % 4}`} aria-hidden="true">
      <span className="art-core">{index % 2 === 0 ? 'GPU' : 'CPU'}</span>
      <span className="art-caption">PERFORMANCE SERIES</span>
    </div>
  )
}

function App() {
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [catalogState, setCatalogState] = useState<LoadState>('loading')
  const [categoryState, setCategoryState] = useState<LoadState>('loading')
  const [catalogError, setCatalogError] = useState('')
  const [categoryError, setCategoryError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [session, setSession] = useState<Session | null>(() => {
    const stored = sessionStorage.getItem('nucleu-session')
    if (!stored) return null
    try {
      return JSON.parse(stored) as Session
    } catch {
      sessionStorage.removeItem('nucleu-session')
      return null
    }
  })
  const [authMode, setAuthMode] = useState<AuthMode | null>(null)
  const [authEmail, setAuthEmail] = useState('')
  const [authPassword, setAuthPassword] = useState('')
  const [authMessage, setAuthMessage] = useState('')
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  const [adminOpen, setAdminOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [productFormOpen, setProductFormOpen] = useState(false)
  const [productDraft, setProductDraft] = useState<ProductDraft>(emptyProduct)
  const [adminError, setAdminError] = useState('')
  const [adminMessage, setAdminMessage] = useState('')
  const [categoryName, setCategoryName] = useState('')
  const [deleteCandidate, setDeleteCandidate] = useState<Product | null>(null)

  const isAdmin = session?.role === 'ADMIN'

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAuthBusy(true)
    setAuthError('')
    setAuthMessage('')
    try {
      if (authMode === 'register') {
        const response = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: authEmail, password: authPassword }),
        })
        if (!response.ok) {
          const error = await response.json().catch(() => ({})) as { detail?: string; message?: string }
          throw new Error(error.detail ?? error.message ?? 'Înregistrarea nu a reușit.')
        }
        await response.json()
        setAuthMessage('Contul a fost creat. Autentifică-te pentru a continua.')
        setAuthMode('login')
        setAuthPassword('')
        return
      }

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail, password: authPassword }),
      })
      if (!response.ok) {
        const error = await response.json().catch(() => ({})) as { detail?: string; message?: string }
        throw new Error(error.detail ?? error.message ?? 'Autentificarea nu a reușit.')
      }
      const token = await response.json() as { accessToken: string; role: 'CLIENT' | 'ADMIN' }
      const loggedIn: Session = { accessToken: token.accessToken, role: token.role, email: authEmail }
      sessionStorage.setItem('nucleu-session', JSON.stringify(loggedIn))
      setSession(loggedIn)
      setAuthMode(null)
      setAuthPassword('')
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Autentificarea nu a reușit.')
    } finally {
      setAuthBusy(false)
    }
  }

  function logout() {
    sessionStorage.removeItem('nucleu-session')
    setSession(null)
    setAdminOpen(false)
  }

  function startNewProduct() {
    setEditingProduct(null)
    setProductFormOpen(true)
    setProductDraft({ ...emptyProduct, category: categories[0]?.name ?? '' })
    setAdminError('')
    setAdminMessage('')
  }

  function startEditProduct(product: Product) {
    setEditingProduct(product)
    setProductFormOpen(true)
    setProductDraft({
      name: product.name,
      description: product.description,
      specifications: product.specifications,
      price: String(product.price),
      stock: String(product.stock),
      supplier: product.supplier,
      deliveryMethod: product.deliveryMethod,
      category: product.category,
    })
    setAdminError('')
    setAdminMessage('')
  }

  async function saveProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!session) return
    setAdminError('')
    setAdminMessage('')
    const payload = {
      ...productDraft,
      price: Number(productDraft.price),
      stock: Number(productDraft.stock),
    }
    try {
      await requestJson<Product>(
        editingProduct ? `/api/products/${editingProduct.id}` : '/api/products',
        session.accessToken,
        editingProduct ? 'PUT' : 'POST',
        payload,
      )
      setEditingProduct(null)
      setProductFormOpen(false)
      setProductDraft(emptyProduct)
      setAdminMessage(editingProduct ? 'Produsul a fost actualizat.' : 'Produsul a fost adăugat în catalog.')
      setRefreshKey((key) => key + 1)
    } catch (error) {
      setAdminError(error instanceof Error ? error.message : 'Salvarea produsului a eșuat.')
    }
  }

  async function deleteProduct() {
    if (!session || !deleteCandidate) return
    try {
      await requestJson<void>(`/api/products/${deleteCandidate.id}`, session.accessToken, 'DELETE')
      setAdminMessage(`Produsul „${deleteCandidate.name}” a fost șters.`)
      setDeleteCandidate(null)
      setRefreshKey((key) => key + 1)
    } catch (error) {
      setAdminError(error instanceof Error ? error.message : 'Ștergerea produsului a eșuat.')
      setDeleteCandidate(null)
    }
  }

  async function createCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!session || !categoryName.trim()) return
    setAdminError('')
    try {
      const created = await requestJson<Category>('/api/categories', session.accessToken, 'POST', { name: categoryName.trim() })
      setCategories((current) => [...current, created])
      setCategoryName('')
      setProductDraft((current) => ({ ...current, category: current.category || created.name }))
      setAdminMessage(`Categoria „${created.name}” a fost adăugată.`)
    } catch (error) {
      setAdminError(error instanceof Error ? error.message : 'Adăugarea categoriei a eșuat.')
    }
  }

  useEffect(() => {
    let active = true
    readJson<Category[]>('/api/categories')
      .then((data) => {
        if (!active) return
        setCategories(data)
        setCategoryState('ready')
      })
      .catch(() => {
        if (!active) return
        setCategoryError('Categoriile nu au putut fi încărcate.')
        setCategoryState('error')
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    const params = new URLSearchParams()
    if (query.trim()) params.set('q', query.trim())
    if (categoryId) params.set('categoryId', categoryId)
    const suffix = params.size ? `?${params.toString()}` : ''

    readJson<Product[]>(`/api/products${suffix}`)
      .then((data) => {
        if (!active) return
        setProducts(data)
        setCatalogState('ready')
      })
      .catch(() => {
        if (!active) return
        setCatalogError('Nu am putut încărca produsele. Verifică dacă API-ul rulează și încearcă din nou.')
        setCatalogState('error')
      })
    return () => { active = false }
  }, [query, categoryId, refreshKey])

  return (
    <div className="storefront">
      <div className="announcement">
        <span>Livrare rapidă, fără surprize</span>
        <span className="announcement-divider">/</span>
        <span>Tehnologie aleasă cu cap</span>
      </div>

      <header className="site-header">
        <a className="wordmark" href="#top" aria-label="Nucleu, pagina principală">
          <span className="wordmark-symbol">N<span>.</span></span>
          <span className="wordmark-copy">NUCLEU<small>PC COMPONENTS</small></span>
        </a>
        <nav className="main-nav" aria-label="Navigare principală">
          <a className="nav-active" href="#catalog">Magazin</a>
          <a href="#catalog">Componente</a>
          <a href="#about">Despre noi</a>
        </nav>
        <div className="header-actions">
          {isAdmin && <button className="admin-nav-button" type="button" onClick={() => setAdminOpen((open) => !open)}>
            {adminOpen ? 'Închide administrarea' : 'Administrare'}
          </button>}
          {session ? <>
            <span className="signed-in-label">{session.email}</span>
            <button className="account-button" type="button" onClick={logout}>Ieșire</button>
          </> : <button className="account-button" type="button" onClick={() => {
            setAuthMode('login')
            setAuthMessage('')
            setAuthError('')
          }}>Intră în cont</button>}
        </div>
      </header>

      {authMode && <div className="modal-backdrop" onMouseDown={(event) => {
        if (event.target === event.currentTarget) setAuthMode(null)
      }}>
        <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
          <button className="dialog-close" type="button" aria-label="Închide" onClick={() => setAuthMode(null)}>×</button>
          <p className="eyebrow"><span className="eyebrow-line" /> CONT NUCLEU</p>
          <h2 id="auth-title">{authMode === 'login' ? 'Bine ai revenit.' : 'Creează-ți contul.'}</h2>
          <p className="auth-description">{authMode === 'login' ? 'Autentifică-te pentru a continua.' : 'Înregistrarea creează un cont de client.'}</p>
          {authMessage && <p className="form-success" role="status">{authMessage}</p>}
          {authError && <p className="form-error" role="alert">{authError}</p>}
          <form className="auth-form" onSubmit={submitAuth}>
            <label>Email<input type="email" autoComplete="email" value={authEmail} onChange={(event) => setAuthEmail(event.target.value)} required maxLength={254} /></label>
            <label>Parolă<input type="password" autoComplete={authMode === 'login' ? 'current-password' : 'new-password'} value={authPassword} onChange={(event) => setAuthPassword(event.target.value)} required minLength={12} maxLength={72} /></label>
            <button className="form-submit" type="submit" disabled={authBusy}>{authBusy ? 'Se procesează...' : authMode === 'login' ? 'Autentificare' : 'Creează cont'}</button>
          </form>
          <p className="auth-switch">{authMode === 'login' ? 'Nu ai cont?' : 'Ai deja cont?'}{' '}
            <button type="button" onClick={() => {
              setAuthMode(authMode === 'login' ? 'register' : 'login')
              setAuthMessage('')
              setAuthError('')
            }}>{authMode === 'login' ? 'Creează cont' : 'Autentifică-te'}</button>
          </p>
        </section>
      </div>}

      <main id="top">
        <section className="hero">
          <div className="hero-copy">
            <p className="eyebrow"><span className="eyebrow-line" /> HARDWARE, FĂRĂ COMPROMISURI</p>
            <h1>Puterea începe<br />din <em>interior.</em></h1>
            <p className="hero-description">
              Componente atent alese pentru ideile care merită mai mult. Construiește-ți următorul sistem, piesă cu piesă.
            </p>
            <a className="primary-link" href="#catalog">
              Descoperă componentele <span aria-hidden="true">↓</span>
            </a>
            <div className="hero-proof">
              <span className="proof-mark">✳</span>
              <span>Performanță pe bune.<br /><strong>Fără zgomot în plus.</strong></span>
            </div>
          </div>
          <div className="hero-visual" aria-label="Ilustrație abstractă de hardware performant">
            <div className="visual-heading">
              <span className="visual-kicker">NUCLEU / COMPONENTE PC</span>
              <strong>BUILD<br /><em>BETTER.</em></strong>
              <span className="visual-subtitle">PIESE BUNE. SISTEME MAI BUNE.</span>
            </div>
            <div className="visual-index">01 <span>/</span> 04</div>
          </div>
        </section>

        <section className="benefit-strip" aria-label="Avantaje">
          <div><span className="benefit-number">01</span><span><strong>Selecție curată</strong><small>Doar ce merită în sistemul tău</small></span></div>
          <div><span className="benefit-number">02</span><span><strong>Specificații clare</strong><small>Știi exact ce cumperi</small></span></div>
          <div><span className="benefit-number">03</span><span><strong>Suport de la oameni</strong><small>Răspunsuri, nu scripturi</small></span></div>
          <span className="strip-mark" aria-hidden="true">N.</span>
        </section>

        {isAdmin && adminOpen && <section className="admin-panel" aria-labelledby="admin-heading">
          <div className="admin-panel-heading">
            <div><p className="eyebrow"><span className="eyebrow-line" /> MOD ADMINISTRATOR</p><h2 id="admin-heading">Administrare catalog</h2></div>
            <button className="new-product-button" type="button" onClick={startNewProduct}>＋ Produs nou</button>
          </div>
          <div className="category-admin">
            <div><strong>Categorii disponibile</strong><span>{categories.map((category) => category.name).join(' · ') || 'Nicio categorie creată încă'}</span></div>
            <form onSubmit={createCategory}>
              <label className="sr-only" htmlFor="new-category">Categorie nouă</label>
              <input id="new-category" value={categoryName} onChange={(event) => setCategoryName(event.target.value)} placeholder="Nume categorie" required maxLength={100} />
              <button type="submit" disabled={!categoryName.trim()}>Adaugă categoria</button>
            </form>
          </div>
          {adminMessage && <p className="form-success" role="status">{adminMessage}</p>}
          {adminError && <p className="form-error" role="alert">{adminError}</p>}
          {productFormOpen ? <form className="product-admin-form" onSubmit={saveProduct}>
            <h3>{editingProduct ? `Editează: ${editingProduct.name}` : 'Produs nou'}</h3>
            <div className="product-form-grid">
              <label>Nume produs<input value={productDraft.name} onChange={(event) => setProductDraft({ ...productDraft, name: event.target.value })} required maxLength={160} /></label>
              <label>Furnizor<input value={productDraft.supplier} onChange={(event) => setProductDraft({ ...productDraft, supplier: event.target.value })} required maxLength={120} /></label>
              <label className="wide-field">Descriere<textarea value={productDraft.description} onChange={(event) => setProductDraft({ ...productDraft, description: event.target.value })} required maxLength={2000} rows={2} /></label>
              <label className="wide-field">Specificații<textarea value={productDraft.specifications} onChange={(event) => setProductDraft({ ...productDraft, specifications: event.target.value })} required maxLength={2000} rows={2} /></label>
              <label>Preț (lei)<input type="number" min="0.01" step="0.01" value={productDraft.price} onChange={(event) => setProductDraft({ ...productDraft, price: event.target.value })} required /></label>
              <label>Stoc<input type="number" min="0" step="1" value={productDraft.stock} onChange={(event) => setProductDraft({ ...productDraft, stock: event.target.value })} required /></label>
              <label>Modalitate de livrare<input value={productDraft.deliveryMethod} onChange={(event) => setProductDraft({ ...productDraft, deliveryMethod: event.target.value })} required maxLength={80} /></label>
              <label>Categorie produs<select aria-label="Categorie produs" value={productDraft.category} onChange={(event) => setProductDraft({ ...productDraft, category: event.target.value })} required>
                <option value="" disabled>Alege categoria</option>
                {categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}
              </select></label>
            </div>
            <div className="product-form-actions">
              <button className="form-submit" type="submit">{editingProduct ? 'Salvează modificările' : 'Salvează produsul'}</button>
              <button className="cancel-button" type="button" onClick={() => { setEditingProduct(null); setProductFormOpen(false); setProductDraft(emptyProduct) }}>Anulează</button>
            </div>
          </form> : <p className="admin-hint">Alege „Produs nou” sau editează un produs din catalog pentru a începe.</p>}
          {deleteCandidate && <div className="delete-backdrop"><section className="delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-heading">
            <h3 id="delete-heading">Ștergi produsul?</h3><p>„{deleteCandidate.name}” va fi eliminat din catalog.</p>
            <button className="delete-confirm" type="button" onClick={deleteProduct}>Confirmă ștergerea</button>
            <button className="cancel-button" type="button" onClick={() => setDeleteCandidate(null)}>Renunță</button>
          </section></div>}
        </section>}

        <section className="catalog-section" id="catalog">
          <div className="catalog-heading">
            <div>
              <p className="eyebrow"><span className="eyebrow-line" /> CATALOGUL NOSTRU</p>
              <h2>Piese cu <em>personalitate.</em></h2>
            </div>
            <p className="catalog-intro">De la primul upgrade până la un build complet — găsește componenta potrivită.</p>
          </div>

          <div className="catalog-toolbar">
            <label className="search-field">
              <span className="search-icon" aria-hidden="true">⌕</span>
              <input
                type="search"
                aria-label="Caută produse"
                placeholder="Caută procesoare, plăci video..."
                value={query}
                onChange={(event) => {
                  setCatalogError('')
                  setCatalogState('loading')
                  setQuery(event.target.value)
                }}
              />
              <span className="search-shortcut">↵</span>
            </label>
            <label className="category-field">
              <span className="sr-only">Categorie</span>
              <select
                aria-label="Categorie"
                value={categoryId}
                onChange={(event) => {
                  setCatalogError('')
                  setCatalogState('loading')
                  setCategoryId(event.target.value)
                }}
                disabled={categoryState !== 'ready'}
              >
                <option value="">Toate categoriile</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
              <span className="select-chevron" aria-hidden="true">⌄</span>
            </label>
          </div>
          {categoryState === 'error' && <p className="inline-error" role="alert">{categoryError}</p>}
          <div className="results-meta">
            <span>{catalogState === 'ready' ? `${products.length} PRODUSE GĂSITE` : 'CATALOG COMPONENTE'}</span>
            <span className="results-rule" />
            <span className="sort-label">SELECTATE CU GRIJĂ <span aria-hidden="true">✳</span></span>
          </div>

          {catalogState === 'loading' && (
            <div className="catalog-message" role="status"><span className="loading-dot" /> Se încarcă produsele...</div>
          )}
          {catalogState === 'error' && (
            <div className="catalog-message catalog-message--error" role="alert">
              <span>{catalogError}</span>
              <button type="button" onClick={() => {
                setCatalogError('')
                setCatalogState('loading')
                setRefreshKey((key) => key + 1)
              }}>Reîncearcă</button>
            </div>
          )}
          {catalogState === 'ready' && products.length === 0 && (
            <div className="catalog-message catalog-message--empty">
              <span className="empty-symbol">⌕</span>
              <strong>N-am găsit piesa potrivită.</strong>
              <span>Încearcă alt termen sau schimbă categoria.</span>
            </div>
          )}
          {catalogState === 'ready' && products.length > 0 && (
            <div className="product-grid">
              {products.map((product, index) => (
                <article className="product-card" key={product.id}>
                  <div className="product-visual-wrap">
                    <ProductArtwork index={index} />
                    <span className="product-category">{product.category}</span>
                  </div>
                  <div className="product-content">
                    <p className="product-maker">{product.supplier}</p>
                    <h3>{product.name}</h3>
                    <p className="product-description">{product.description}</p>
                    <p className="product-specs">{product.specifications}</p>
                  <div className="product-bottom">
                      <div className="product-price"><strong>{formatPrice(product.price)}</strong><span>LEI</span></div>
                      <span className={`stock-status ${product.stock > 0 ? 'is-available' : 'is-unavailable'}`}>
                        <i />{product.stock > 0 ? `În stoc · ${product.stock}` : 'Stoc epuizat'}
                      </span>
                  </div>
                  {isAdmin && adminOpen && <div className="product-admin-actions">
                    <button type="button" aria-label={`Editează ${product.name}`} onClick={() => startEditProduct(product)}>Editează</button>
                    <button type="button" aria-label={`Șterge ${product.name}`} onClick={() => setDeleteCandidate(product)}>Șterge</button>
                  </div>}
                </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      <footer id="about" className="site-footer">
        <a className="wordmark wordmark--footer" href="#top"><span className="wordmark-symbol">N<span>.</span></span><span className="wordmark-copy">NUCLEU<small>PC COMPONENTS</small></span></a>
        <span>Hardware ales cu cap. <span className="footer-dot">●</span> Proiect demonstrativ</span>
        <a href="http://localhost:8080/swagger-ui/index.html" target="_blank" rel="noreferrer">API DOCUMENTATION ↗</a>
      </footer>
    </div>
  )
}

export default App
