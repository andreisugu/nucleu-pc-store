import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

const products = [
  {
    id: 1,
    name: 'GeForce RTX 5080',
    description: 'Placă video pentru gaming',
    specifications: '16 GB GDDR7',
    price: 5999.99,
    stock: 3,
    supplier: 'Demo Parts',
    deliveryMethod: 'Curier',
    category: 'Plăci video',
  },
  {
    id: 2,
    name: 'Ryzen 7 9800X3D',
    description: 'Procesor pentru gaming',
    specifications: '8 nuclee',
    price: 2199.99,
    stock: 0,
    supplier: 'AMD',
    deliveryMethod: 'Curier',
    category: 'Procesoare',
  },
]

describe('catalog storefront', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), window.location.origin)
      if (url.pathname === '/api/categories') {
        return Response.json([
          { id: 1, name: 'Plăci video' },
          { id: 2, name: 'Procesoare' },
        ])
      }
      const query = url.searchParams.get('q')?.toLocaleLowerCase('ro') ?? ''
      const category = url.searchParams.get('categoryId')
      const filtered = products.filter((product) =>
        (!query || `${product.name} ${product.description} ${product.specifications}`
          .toLocaleLowerCase('ro').includes(query))
        && (!category || product.category === (category === '1' ? 'Plăci video' : 'Procesoare')),
      )
      return Response.json(filtered)
    }))
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('loads products from the API and shows stock and prices', async () => {
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'GeForce RTX 5080' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ryzen 7 9800X3D' })).toBeInTheDocument()
    expect(screen.getByText('5.999,99')).toBeInTheDocument()
    expect(screen.getByText(/Stoc epuizat/i)).toBeInTheDocument()
  })

  it('filters the catalog by search text and category', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'GeForce RTX 5080' })

    await user.type(screen.getByRole('searchbox', { name: /caută/i }), 'ryzen')
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'GeForce RTX 5080' })).not.toBeInTheDocument())
    expect(screen.getByRole('heading', { name: 'Ryzen 7 9800X3D' })).toBeInTheDocument()

    await user.clear(screen.getByRole('searchbox', { name: /caută/i }))
    await user.selectOptions(screen.getByRole('combobox', { name: /categorie/i }), '1')
    await waitFor(() => expect(screen.getByRole('heading', { name: 'GeForce RTX 5080' })).toBeInTheDocument())
    expect(screen.queryByRole('heading', { name: 'Ryzen 7 9800X3D' })).not.toBeInTheDocument()
  })

  it('shows a useful empty state when the API returns no matches', async () => {
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { name: 'GeForce RTX 5080' })

    await user.type(screen.getByRole('searchbox', { name: /caută/i }), 'produs inexistent')

    expect(await screen.findByText('N-am găsit piesa potrivită.')).toBeInTheDocument()
  })

  it('shows a recoverable error when the catalog API is unavailable', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), window.location.origin)
      if (url.pathname === '/api/categories') return Response.json([])
      return new Response(null, { status: 503 })
    }))
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('API-ul rulează')
    expect(screen.getByRole('button', { name: 'Reîncearcă' })).toBeInTheDocument()
  })
})
