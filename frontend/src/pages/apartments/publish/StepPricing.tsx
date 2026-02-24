import { normalizePriceInput, type PublishFormData } from './publishForm'

const BILLS = [
  { key: 'agua', label: 'Agua', icon: '💧' },
  { key: 'luz', label: 'Luz', icon: '💡' },
  { key: 'gas', label: 'Gas', icon: '🔥' },
  { key: 'internet', label: 'Internet', icon: '📶' },
  { key: 'comunidad', label: 'Comunidad', icon: '🏢' },
  { key: 'seguro', label: 'Seguro', icon: '🛡️' },
]

const WRAPPER_CLASS = 'flex flex-col gap-8'
const PRICE_INPUT_CLASS =
  'input input-bordered text-4xl font-extrabold text-base-content w-40 text-center rounded-xl bg-base-100 focus:outline-primary tracking-tight'
const DEPOSIT_BUTTON_CLASS =
  'w-11 h-11 rounded-full bg-base-300/70 hover:bg-base-300 flex items-center justify-center text-xl font-bold text-base-content transition'
const DATE_INPUT_CLASS = 'input input-bordered w-full rounded-xl bg-base-100 focus:outline-primary'
const BILL_CHIP_BASE_CLASS =
  'flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium transition-all'
const BILL_CHIP_ACTIVE_CLASS = 'bg-primary text-primary-content shadow-sm'
const BILL_CHIP_INACTIVE_CLASS = 'bg-base-300/60 text-base-content/70 hover:bg-base-300'

interface Props {
  data: PublishFormData
  updateFields: (fields: Partial<PublishFormData>) => void
}

export default function StepPricing({ data, updateFields }: Props) {
  const handlePriceChange = (value: string) => {
    const normalized = normalizePriceInput(value)
    if (normalized === null) return
    updateFields({ priceInput: normalized })
  }

  const decreaseDeposit = () => {
    updateFields({ deposit: Math.max(0, data.deposit - 1) })
  }

  const increaseDeposit = () => {
    updateFields({ deposit: Math.min(12, data.deposit + 1) })
  }

  const toggleBill = (key: string) => {
    const next = data.includedBills.includes(key)
      ? data.includedBills.filter((b) => b !== key)
      : [...data.includedBills, key]
    updateFields({ includedBills: next })
  }

  return (
    <div className={WRAPPER_CLASS}>
      <div>
        <label className="text-sm font-semibold text-base-content block mb-2">Precio mensual</label>
        <div className="flex items-end gap-3">
          <input
            type="text"
            inputMode="decimal"
            value={data.priceInput}
            onChange={(e) => {
              handlePriceChange(e.target.value)
            }}
            placeholder="450"
            className={PRICE_INPUT_CLASS}
          />
          <span className="text-lg font-semibold text-base-content/60 mb-2">€ / mes</span>
        </div>
        <p className="text-xs text-base-content/50 mt-1.5">
          El precio medio en tu zona es de aprox. 420&nbsp;€ – 520&nbsp;€ / mes.
        </p>
      </div>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-2">Fianza (meses)</label>
        <div className="flex items-center gap-4">
          <button type="button" onClick={decreaseDeposit} className={DEPOSIT_BUTTON_CLASS}>
            −
          </button>
          <span className="text-2xl font-bold text-base-content w-8 text-center">
            {data.deposit}
          </span>
          <button type="button" onClick={increaseDeposit} className={DEPOSIT_BUTTON_CLASS}>
            +
          </button>
        </div>
      </div>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-2">
          Fecha disponible
        </label>
        <input
          type="date"
          value={data.availableDate}
          onChange={(e) => {
            updateFields({ availableDate: e.target.value })
          }}
          className={DATE_INPUT_CLASS}
        />
      </div>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-2">
          Gastos incluidos
        </label>
        <div className="flex flex-wrap gap-2">
          {BILLS.map(({ key, label, icon }) => {
            const isActive = data.includedBills.includes(key)
            return (
              <button
                key={key}
                type="button"
                onClick={() => {
                  toggleBill(key)
                }}
                className={`${BILL_CHIP_BASE_CLASS}
                  ${isActive ? BILL_CHIP_ACTIVE_CLASS : BILL_CHIP_INACTIVE_CLASS}`}
              >
                <span>{icon}</span>
                {label}
              </button>
            )
          })}
        </div>
      </div>
    </div>
  )
}
