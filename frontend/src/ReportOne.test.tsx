import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

type ApiProduct = {
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

describe('Raportarea I complete storefront flow', () => {
  let products: ApiProduct[]
  let requests: Array<{ url: string; init?: RequestInit }> 

  beforeEach(() => {
    products = [{
      id: 7,
      name: 'RTX demo',
      description: 'GPU demo',
      specifications: '16 GB',
      price: 3500,
      stock: 5,
      supplier: 'NVIDIA',
      deliveryMethod: 'Curier',
      category: 'Plăci video',
    }]
    requests = []
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), window.location.origin)
      requests.push({ url: `${url.pathname}${url.search}`, init })

      if (url.pathname === '/api/categories' && (!init || init.method === 'GET')) {
        return Response.json([{ id: 1, name: 'Plăci video' }, { id: 2, name: 'Procesoare' }])
      }
      if (url.pathname === '/api/products' && (!init || init.method === 'GET')) return Response.json(products)
      if (url.pathname === '/api/auth/register') {
        return Response.json({ email: 'client@example.test', role: 'CLIENT' }, { status: 201 })
      }
      if (url.pathname === '/api/auth/login') {
        const body = JSON.parse(String(init?.body)) as { email: string }
        const role = body.email.startsWith('admin') ? 'ADMIN' : 'CLIENT'
        return Response.json({ accessToken: `token-${role}`, tokenType: 'Bearer', expiresIn: 3600, role })
      }
      if (url.pathname === '/api/products' && init?.method === 'POST') {
        const product = { ...JSON.parse(String(init.body)), id: 8 } as ApiProduct
        products = [...products, product]
        return Response.json(product, { status: 201 })
      }
      const productMatch = url.pathname.match(/^\/api\/products\/(\d+)$/)
      if (productMatch && init?.method === 'PUT') {
        const id = Number(productMatch[1])
        const updated = { ...JSON.parse(String(init.body)), id } as ApiProduct
        products = products.map((product) => product.id === id ? updated : product)
        return Response.json(updated)
      }
      if (productMatch && init?.method === 'DELETE') {
        products = products.filter((product) => product.id !== Number(productMatch[1]))
        return new Response(null, { status: 204 })
      }
      return new Response('Unexpected API request', { status: 404 })
    }))
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('lets a visitor register and log in as a client without admin access', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'RTX demo' })

    await user.click(screen.getByRole('button', { name: /intră în cont/i }))
    await user.click(screen.getByRole('button', { name: /creează cont/i }))
    await user.type(screen.getByLabelText('Email'), 'client@example.test')
    await user.type(screen.getByLabelText('Parolă'), 'ClientPass123!')
    await user.click(screen.getByRole('button', { name: 'Creează cont' }))
    expect(await screen.findByRole('status')).toHaveTextContent(/contul a fost creat/i)

    await user.type(screen.getByLabelText('Parolă'), 'ClientPass123!')
    await user.click(screen.getByRole('button', { name: 'Autentificare' }))

    expect(await screen.findByText('client@example.test')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /administrare/i })).not.toBeInTheDocument()
    expect(requests.some((request) => request.url === '/api/auth/register')).toBe(true)
  })

  it('lets an admin create, edit and delete a product through protected endpoints', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'RTX demo' })
    await user.click(screen.getByRole('button', { name: /intră în cont/i }))
    await user.type(screen.getByLabelText('Email'), 'admin@example.test')
    await user.type(screen.getByLabelText('Parolă'), 'AdminPass123!')
    await user.click(screen.getByRole('button', { name: 'Autentificare' }))
    await user.click(await screen.findByRole('button', { name: /administrare/i }))

    await user.click(screen.getByRole('button', { name: /produs nou/i }))
    await user.type(screen.getByLabelText('Nume produs'), 'Ryzen 9 demo')
    await user.type(screen.getByLabelText('Descriere'), 'Procesor demonstrativ')
    await user.type(screen.getByLabelText('Specificații'), '16 nuclee')
    await user.type(screen.getByLabelText('Preț (lei)'), '2499.99')
    await user.type(screen.getByLabelText('Stoc'), '4')
    await user.type(screen.getByLabelText('Furnizor'), 'AMD')
    await user.type(screen.getByLabelText('Modalitate de livrare'), 'Curier')
    await user.selectOptions(screen.getByLabelText('Categorie produs'), 'Procesoare')
    await user.click(screen.getByRole('button', { name: 'Salvează produsul' }))

    expect(await screen.findByRole('heading', { name: 'Ryzen 9 demo' })).toBeInTheDocument()
    const createRequest = requests.find((request) => request.url === '/api/products' && request.init?.method === 'POST')
    expect(createRequest?.init?.headers).toMatchObject({ Authorization: 'Bearer token-ADMIN' })
    expect(JSON.parse(String(createRequest?.init?.body))).toMatchObject({
      name: 'Ryzen 9 demo', price: 2499.99, stock: 4, category: 'Procesoare',
    })

    await user.click(screen.getByRole('button', { name: /editează rtx demo/i }))
    const nameInput = screen.getByLabelText('Nume produs')
    await user.clear(nameInput)
    await user.type(nameInput, 'RTX demo revizuit')
    await user.click(screen.getByRole('button', { name: 'Salvează modificările' }))
    expect(await screen.findByRole('heading', { name: 'RTX demo revizuit' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /șterge rtx demo revizuit/i }))
    await user.click(screen.getByRole('button', { name: 'Confirmă ștergerea' }))
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'RTX demo revizuit' })).not.toBeInTheDocument())
    expect(requests.some((request) => request.url === '/api/products/7' && request.init?.method === 'PUT')).toBe(true)
    expect(requests.some((request) => request.url === '/api/products/7' && request.init?.method === 'DELETE')).toBe(true)
  })
})
