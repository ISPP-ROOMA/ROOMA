import { useMemo, useState } from 'react'

type RequestStatus = 'pending' | 'accepted' | 'cancelled'

type RequestItem = {
  id: number
  title: string
  location: string
  price: string
  appliedAt: string
  status: RequestStatus
  imageUrl: string
  isMatch: boolean
}

const requests: RequestItem[] = [
  {
    id: 1,
    title: 'Piso en Los Remedios',
    location: 'Calle Asunción, Sevilla',
    price: '1.100 €',
    appliedAt: 'Aplicado el 12 Oct',
    status: 'pending',
    imageUrl:
      'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
    isMatch: false,
  },
  {
    id: 2,
    title: 'Habitación en Nervión',
    location: 'Zona Nervión, Sevilla',
    price: '450 €',
    appliedAt: 'Aplicado el 10 Oct',
    status: 'accepted',
    imageUrl:
      'https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&w=1200&q=80',
    isMatch: true,
  },
  {
    id: 3,
    title: 'Estudio en Santa Cruz',
    location: 'Centro, Sevilla',
    price: '750 €',
    appliedAt: '',
    status: 'cancelled',
    imageUrl:
      'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=1200&q=80',
    isMatch: false,
  },
]

function statusLabel(status: RequestStatus): string {
  switch (status) {
    case 'pending':
      return 'Pendiente'
    case 'accepted':
      return 'Aceptada'
    case 'cancelled':
      return 'Cancelada'
  }
}

function statusClass(status: RequestStatus): string {
  switch (status) {
    case 'pending':
      return 'border border-[#050505] bg-white text-[#050505]'
    case 'accepted':
      return 'border-0 bg-[#008080] text-white'
    case 'cancelled':
      return 'border border-[#DDDBCB] bg-[#F5F1E3] text-[#050505]/60'
  }
}

export default function MyRequests() {
  const [activeTab, setActiveTab] = useState<'pending' | 'match'>('pending')

  const visibleItems = useMemo(() => {
    if (activeTab === 'match') {
      return requests.filter((item) => item.isMatch)
    }
    return requests
  }, [activeTab])

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-md min-h-dvh bg-[#F5F1E3] text-[#050505] pb-28"
    >
      <header className="sticky top-0 z-10 bg-[#F5F1E3] px-4 pt-5 pb-4">
        <div className="flex items-center justify-between">
          <button
            className="h-10 w-10 rounded-full bg-white text-[#050505] shadow-sm flex items-center justify-center"
            aria-label="Volver"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>
          <h1 className="text-3xl font-bold text-[#050505]">Mis Solicitudes</h1>
          <button
            className="h-10 w-10 rounded-full bg-white text-[#050505] shadow-sm flex items-center justify-center"
            aria-label="Notificaciones"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5" />
              <path d="M10 17a2 2 0 0 0 4 0" />
            </svg>
          </button>
        </div>
      </header>

      <section className="px-4">
        <div className="flex rounded-xl bg-[#DDDBCB] p-1">
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium ${activeTab === 'pending' ? 'bg-white text-[#050505]' : 'text-[#050505]/70'}`}
            onClick={() => setActiveTab('pending')}
          >
            Pendientes
          </button>
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium ${activeTab === 'match' ? 'bg-white text-[#050505]' : 'text-[#050505]/70'}`}
            onClick={() => setActiveTab('match')}
          >
            Match
          </button>
        </div>
      </section>

      <section className="mt-5 space-y-4 px-4">
        {visibleItems.length === 0 && (
          <article className="rounded-2xl border border-[#DDDBCB] bg-white p-6 text-center text-[#050505]/70">
            Todavía no tienes solicitudes en esta pestaña.
          </article>
        )}

        {visibleItems.map((item) => (
          <article
            key={item.id}
            className={`overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm ${item.status === 'cancelled' ? 'opacity-70' : ''}`}
          >
            <div
              className={`relative h-44 w-full ${item.status === 'cancelled' ? 'grayscale' : ''}`}
            >
              <img src={item.imageUrl} alt={item.title} className="h-full w-full object-cover" />
              {item.status === 'cancelled' && (
                <span className="absolute right-3 top-3 rounded-lg bg-white/90 px-2 py-1 text-sm font-semibold text-[#050505]/60">
                  Cancelada
                </span>
              )}
            </div>

            <div className="p-4">
              <div className="flex items-start justify-between gap-2">
                <h2 className="text-3xl font-bold leading-tight text-[#050505]">{item.title}</h2>
                <span
                  className={`text-xl font-semibold ${item.status === 'cancelled' ? 'text-[#050505]/60' : 'text-[#008080]'}`}
                >
                  {item.price}
                </span>
              </div>

              <p className="text-[#050505]/70">{item.location}</p>

              <div className="my-3 h-px w-full bg-[#DDDBCB]" />

              <div className="flex items-center justify-between gap-2 text-sm">
                <span className={`rounded-full px-3 py-1 text-sm ${statusClass(item.status)}`}>
                  {statusLabel(item.status)}
                </span>
                <span className="text-[#050505]/60">{item.appliedAt}</span>
                <span className="text-2xl text-[#050505]">›</span>
              </div>
            </div>
          </article>
        ))}
      </section>

      <nav className="fixed bottom-0 left-0 right-0 mx-auto w-full max-w-md border-t border-[#DDDBCB] bg-white">
        <ul className="grid grid-cols-5 py-2 text-xs">
          <li className="text-center text-[#050505]/60">Explorar</li>
          <li className="text-center text-[#050505]/60">Favoritos</li>
          <li className="text-center font-semibold text-[#008080]">Solicitudes</li>
          <li className="text-center text-[#050505]/60">Mensajes</li>
          <li className="text-center text-[#050505]/60">Perfil</li>
        </ul>
      </nav>
    </div>
  )
}
