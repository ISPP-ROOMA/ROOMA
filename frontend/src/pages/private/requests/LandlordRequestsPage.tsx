import { useEffect, useMemo, useState } from 'react'
import {
  getReceivedRequests,
  type RequestItem,
  type RequestStatus,
} from '../../../service/requests.service'

function statusClass(status: RequestStatus): string {
  switch (status) {
    case 'PENDING':
      return 'badge badge-warning'
    case 'ON_HOLD':
      return 'badge badge-info'
    case 'ACCEPTED':
      return 'badge badge-success'
    case 'REJECTED':
      return 'badge badge-error'
    case 'CANCELLED':
      return 'badge badge-neutral'
  }
}

function statusLabel(status: RequestStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Pendiente'
    case 'ACCEPTED':
      return 'Aceptada'
    case 'ON_HOLD':
      return 'En espera'
    case 'REJECTED':
      return 'Rechazada'
    case 'CANCELLED':
      return 'Cancelada'
  }
}

export default function LandlordRequestsPage() {
  const [requests, setRequests] = useState<RequestItem[]>([])
  const [loading, setLoading] = useState(true)

  const updateRequestStatus = (requestId: number, status: RequestStatus) => {
    setRequests((prevRequests) =>
      prevRequests.map((request) =>
        request.id === requestId ? { ...request, status } : request
      )
    )
  }

  useEffect(() => {
    void getReceivedRequests()
      .then((data) => {
        setRequests(data)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  const pendingCount = useMemo(
    () => requests.filter((request) => request.status === 'PENDING').length,
    [requests]
  )

  return (
    <div className="container mx-auto px-4 py-6 max-w-5xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Solicitudes recibidas</h1>
        <p className="text-base-content/70 mt-1">
          Revisa todas las solicitudes que han llegado a tus inmuebles.
        </p>
        <div className="mt-3 badge badge-outline">Pendientes: {pendingCount}</div>
      </div>

      {loading && (
        <div className="flex justify-center py-12">
          <span className="loading loading-dots loading-lg" />
        </div>
      )}

      {!loading && requests.length === 0 && (
        <div className="alert">
          <span>No has recibido solicitudes todavía.</span>
        </div>
      )}

      {!loading && requests.length > 0 && (
        <div className="grid gap-4">
          {requests.map((request) => (
            <article key={request.id} className="card bg-base-100 shadow-sm border border-base-200">
              <div className="card-body">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <h2 className="card-title">{request.apartmentTitle}</h2>
                    <p className="text-sm text-base-content/70">{request.apartmentAddress}</p>
                  </div>
                  <span className={statusClass(request.status)}>{statusLabel(request.status)}</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
                  <p>
                    <span className="font-semibold">Inquilino:</span> {request.tenantName}
                  </p>
                  <p>
                    <span className="font-semibold">Recibida:</span> {request.createdAt}
                  </p>
                </div>

                {request.monthlyPrice && (
                  <p className="text-sm">
                    <span className="font-semibold">Precio publicado:</span> {request.monthlyPrice}{' '}
                    €/mes
                  </p>
                )}

                {request.status === 'PENDING' && (
                  <div className="card-actions justify-end mt-2">
                    <button
                      className="btn btn-sm btn-error btn-outline"
                      onClick={() => updateRequestStatus(request.id, 'REJECTED')}
                    >
                      Rechazar
                    </button>
                    <button
                      className="btn btn-sm btn-success"
                      onClick={() => updateRequestStatus(request.id, 'ACCEPTED')}
                    >
                      Aceptar
                    </button>
                  </div>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
