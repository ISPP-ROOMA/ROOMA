const WRAPPER_CLASS = 'flex flex-col items-center justify-center py-16 text-center gap-5'
const ICON_ROW_CLASS = 'flex items-center gap-4'
const ICON_WRAPPER_CLASS = 'w-16 h-16 rounded-full bg-base-300/60 flex items-center justify-center'
const BADGE_CLASS =
  'badge badge-lg bg-primary/10 text-primary border-0 font-semibold tracking-wide text-xs uppercase px-4 py-3'
const DESCRIPTION_CLASS = 'text-sm text-base-content/50 mt-1 max-w-sm mx-auto leading-relaxed'

export default function StepRules() {
  return (
    <div className={WRAPPER_CLASS}>
      <div className={ICON_ROW_CLASS}>
        <div className={ICON_WRAPPER_CLASS}>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-8 w-8 text-base-content/30"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M12 21c-1.5 0-4-1.5-4-4 0-1.5 1-3 2-4s2-2.5 2-4a4 4 0 118 0c0 1.5-1 3-2 4s-2 2.5-2 4c0 2.5-2.5 4-4 4z"
            />
            <circle cx="7" cy="8" r="1.5" strokeWidth={1.5} />
            <circle cx="17" cy="8" r="1.5" strokeWidth={1.5} />
            <circle cx="5" cy="13" r="1.5" strokeWidth={1.5} />
            <circle cx="19" cy="13" r="1.5" strokeWidth={1.5} />
          </svg>
        </div>

        <div className={ICON_WRAPPER_CLASS}>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-8 w-8 text-base-content/30"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
            />
          </svg>
        </div>
      </div>

      <span className={BADGE_CLASS}>En desarrollo</span>

      <div>
        <h3 className="text-lg font-bold text-base-content">Reglas y matching inteligente</h3>
        <p className={DESCRIPTION_CLASS}>
          La configuración de reglas de convivencia y el algoritmo de matching inteligente estarán
          disponibles pronto.
        </p>
      </div>
    </div>
  )
}
