import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyDebts, payDebt, type TenantDebtDTO } from '../../service/billing.service'

const currencyFormatter = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
  minimumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('es-ES', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

const formatCurrency = (value: number) => currencyFormatter.format(value)

const formatDate = (value?: string) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return dateFormatter.format(date)
}

export default function Invoices() {
  const [debts, setDebts] = useState<TenantDebtDTO[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [payingId, setPayingId] = useState<number | null>(null)

  const loadDebts = async () => {
    setIsLoading(true)
    const data = await getMyDebts()
    const safeData = Array.isArray(data) ? data : []
    setDebts(safeData)
    setIsLoading(false)
  }

  useEffect(() => {
    void loadDebts()
  }, [])

  const pendingDebts = useMemo(() => debts.filter((debt) => debt.status === 'PENDING'), [debts])

  const summary = useMemo(() => {
    const totalPending = pendingDebts.reduce((acc, debt) => acc + Number(debt.amount ?? 0), 0)
    const nextDue = pendingDebts
      .map((debt): { date?: string; reference?: string } => ({
        date: debt.bill?.duDate,
        reference: debt.bill?.reference,
      }))
      .filter(
        (item): item is { date: string; reference?: string } => typeof item.date === 'string'
      )
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())[0]

    return {
      pendingCount: pendingDebts.length,
      totalPending,
      nextDueDate: nextDue?.date,
      nextReference: nextDue?.reference,
    }
  }, [pendingDebts])

  const handlePay = async (debtId: number) => {
    setPayingId(debtId)
    const updated = await payDebt(debtId)
    if (updated) {
      setDebts((prev) =>
        Array.isArray(prev)
          ? prev.map((debt) => (debt.id === updated.id ? updated : debt))
          : []
      )
    }
    setPayingId(null)
  }

  return (
    <section className="min-h-[70vh] bg-base-200 py-8 px-4">
      <div className="max-w-6xl mx-auto space-y-6">
        <header className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm uppercase tracking-wide text-primary/80">Controla tus gastos</p>
            <h1 className="text-3xl font-semibold">Facturas y pagos</h1>
            <p className="text-sm text-base-content/70">
              Consulta el histórico de facturas y liquida tus deudas pendientes sin salir de Rooma.
            </p>
          </div>
          <Link to="/my-home" className="btn btn-ghost btn-sm self-start md:self-center">
            ← Volver a mi piso
          </Link>
        </header>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <article className="card bg-white shadow-sm">
            <div className="card-body">
              <p className="text-sm text-base-content/60">Importe pendiente</p>
              <p className="text-3xl font-semibold">{formatCurrency(summary.totalPending)}</p>
              <span className="badge badge-warning badge-outline">
                {summary.pendingCount} deudas
              </span>
            </div>
          </article>
          <article className="card bg-white shadow-sm">
            <div className="card-body">
              <p className="text-sm text-base-content/60">Próximo vencimiento</p>
              <p className="text-2xl font-semibold">
                {summary.nextDueDate ? formatDate(summary.nextDueDate) : 'Sin deudas'}
              </p>
              <span className="text-xs text-base-content/60">
                Ref. {summary.nextReference ?? '—'}
              </span>
            </div>
          </article>
          <article className="card bg-white shadow-sm">
            <div className="card-body">
              <p className="text-sm text-base-content/60">Facturas registradas</p>
              <p className="text-3xl font-semibold">{debts.length}</p>
              <span className="text-xs text-base-content/50">
                Pagadas: {debts.filter((d) => d.status === 'PAID').length}
              </span>
            </div>
          </article>
        </div>

        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="border-b px-6 py-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold">Listado de deudas</h2>
            <button className="btn btn-sm" onClick={() => void loadDebts()} disabled={isLoading}>
              {isLoading ? 'Actualizando…' : 'Actualizar'}
            </button>
          </div>

          {isLoading ? (
            <div className="p-6 text-center text-base-content/60">Cargando facturas…</div>
          ) : debts.length === 0 ? (
            <div className="p-6 text-center text-base-content/60">
              Todavía no tienes facturas asociadas.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr className="text-xs uppercase text-base-content/50">
                    <th>Referencia</th>
                    <th>Vencimiento</th>
                    <th>Importe</th>
                    <th>Estado</th>
                    <th className="text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {debts.map((debt) => (
                    <tr key={debt.id}>
                      <td className="font-semibold">{debt.bill?.reference ?? '—'}</td>
                      <td>{formatDate(debt.bill?.duDate)}</td>
                      <td>{formatCurrency(Number(debt.amount ?? 0))}</td>
                      <td>
                        <span
                          className={`badge ${debt.status === 'PENDING' ? 'badge-warning' : 'badge-success'}`}
                        >
                          {debt.status === 'PENDING' ? 'Pendiente' : 'Pagada'}
                        </span>
                      </td>
                      <td className="text-right">
                        {debt.status === 'PENDING' ? (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => void handlePay(debt.id)}
                            disabled={payingId === debt.id}
                          >
                            {payingId === debt.id ? 'Procesando…' : 'Pagar'}
                          </button>
                        ) : (
                          <span className="text-xs text-base-content/50">Sin acciones</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
