const WRAPPER_CLASS = "flex flex-col items-center justify-center py-16 text-center gap-5"
const ICON_WRAPPER_CLASS = "w-24 h-24 rounded-full bg-base-300/60 flex items-center justify-center"
const BADGE_CLASS = "badge badge-lg bg-primary/10 text-primary border-0 font-semibold tracking-wide text-xs uppercase px-4 py-3"
const DESCRIPTION_CLASS = "text-sm text-base-content/50 mt-1 max-w-xs mx-auto leading-relaxed"

export default function StepPhotos() {
  return (
    <div className={WRAPPER_CLASS}>
      <div className={ICON_WRAPPER_CLASS}>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-12 w-12 text-base-content/30"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M3 9a2 2 0 012-2h1.22l.88-1.76A2 2 0 019 4h6a2 2 0 011.9 1.24L17.78 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
          />
          <circle cx="12" cy="13" r="3" strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} />
        </svg>
      </div>

      <span className={BADGE_CLASS}>
        En desarrollo
      </span>

      <div>
        <h3 className="text-lg font-bold text-base-content">Subida de fotos próximamente</h3>
        <p className={DESCRIPTION_CLASS}>
          Funcionalidad de subida de fotos en desarrollo. Haz clic en <span className="font-semibold">Siguiente</span> para continuar.
        </p>
      </div>
    </div>
  )
}
