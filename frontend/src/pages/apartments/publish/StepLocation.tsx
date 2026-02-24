import type { ChangeEvent } from 'react'
import type { PublishFormData } from './publishForm'

const BARRIOS = [
  'Triana',
  'Macarena',
  'Nervión',
  'Centro',
  'Los Remedios',
  'Santa Cruz',
  'San Bernardo',
  'La Cartuja',
]

const WRAPPER_CLASS = 'flex flex-col gap-6'
const MAP_CLASS =
  'relative w-full aspect-[16/10] bg-base-300/60 rounded-2xl flex items-center justify-center overflow-hidden'
const GRID_CLASS = 'absolute inset-0 opacity-10'
const LOCATION_BUTTON_CLASS =
  'flex items-center gap-2 text-primary font-semibold text-sm self-start hover:opacity-80 transition'
const STREET_INPUT_CLASS =
  'input input-bordered w-full pl-11 rounded-xl bg-base-100 focus:outline-primary'
const CHIP_BASE_CLASS = 'px-4 py-2 rounded-full text-sm font-medium transition-all'
const CHIP_ACTIVE_CLASS = 'bg-primary text-primary-content shadow-sm'
const CHIP_INACTIVE_CLASS = 'bg-base-300/60 text-base-content/70 hover:bg-base-300'

interface Props {
  data: PublishFormData
  updateFields: (fields: Partial<PublishFormData>) => void
}

export default function StepLocation({ data, updateFields }: Props) {
  const handleStreetChange = (event: ChangeEvent<HTMLInputElement>) => {
    updateFields({ street: event.target.value })
  }

  const handleNeighborhoodSelect = (neighborhood: string) => {
    updateFields({ neighborhood })
  }

  return (
    <div className={WRAPPER_CLASS}>
      <div className={MAP_CLASS}>
        <div className={GRID_CLASS}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={`h-${i}`}
              className="absolute w-full border-t border-base-content"
              style={{ top: `${(i + 1) * 16}%` }}
            />
          ))}
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={`v-${i}`}
              className="absolute h-full border-l border-base-content"
              style={{ left: `${(i + 1) * 16}%` }}
            />
          ))}
        </div>

        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-12 w-12 text-primary drop-shadow-lg"
          viewBox="0 0 24 24"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5A2.5 2.5 0 1112 6.5a2.5 2.5 0 010 5z"
            clipRule="evenodd"
          />
        </svg>
      </div>

      <button type="button" className={LOCATION_BUTTON_CLASS}>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
        Usar mi ubicación actual
      </button>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-1.5">
          Calle y número
        </label>
        <div className="relative">
          <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"
              />
            </svg>
          </span>
          <input
            type="text"
            value={data.street}
            onChange={handleStreetChange}
            placeholder="Ej. Calle Mayor, 10"
            className={STREET_INPUT_CLASS}
          />
        </div>
      </div>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-2">Barrio</label>
        <div className="flex flex-wrap gap-2">
          {BARRIOS.map((barrio) => {
            const isActive = data.neighborhood === barrio
            return (
              <button
                key={barrio}
                type="button"
                onClick={() => handleNeighborhoodSelect(barrio)}
                className={`${CHIP_BASE_CLASS}
                  ${isActive ? CHIP_ACTIVE_CLASS : CHIP_INACTIVE_CLASS}`}
              >
                {barrio}
              </button>
            )
          })}
        </div>
      </div>
    </div>
  )
}
