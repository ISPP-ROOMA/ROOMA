import { useNavigate, useParams } from 'react-router-dom'

export default function LeaveReview() {
  const { contractId } = useParams()
  const navigate = useNavigate()

  return (
    <div className="container mx-auto p-4 max-w-3xl mt-8">
      <div className="mb-6 flex items-center gap-4">
        <button
          onClick={() => {
            navigate(-1)
          }}
          className="btn btn-ghost btn-sm"
        >
          &larr; Volver
        </button>
        <h1 className="text-3xl font-bold">Valorar Contrato #{contractId}</h1>
      </div>

      <div className="card bg-base-100 shadow-xl border border-base-200">
        <div className="card-body">
          <h2 className="card-title text-primary">Formulario de Valoración</h2>

          <div className="space-y-6 opacity-60 pointer-events-none mt-2">
            <div className="form-control">
              <label className="label">
                <span className="label-text font-semibold uppercase text-xs tracking-wider">
                  Valoración del Casero
                </span>
              </label>
              <div className="rating">
                <input type="radio" name="rating-2" className="mask mask-star-2 bg-orange-400" />
                <input type="radio" name="rating-2" className="mask mask-star-2 bg-orange-400" />
                <input type="radio" name="rating-2" className="mask mask-star-2 bg-orange-400" />
                <input
                  type="radio"
                  name="rating-2"
                  className="mask mask-star-2 bg-orange-400"
                  defaultChecked
                />
                <input type="radio" name="rating-2" className="mask mask-star-2 bg-orange-400" />
              </div>
              <textarea
                className="textarea textarea-bordered h-24 mt-3"
                placeholder="Deja un comentario sobre el casero..."
              ></textarea>
            </div>

            <div className="divider"></div>

            <div className="form-control">
              <label className="label">
                <span className="label-text font-semibold uppercase text-xs tracking-wider">
                  Valoración de los Inquilinos (Compañeros)
                </span>
              </label>
              <div className="rating">
                <input type="radio" name="rating-3" className="mask mask-star-2 bg-orange-400" />
                <input type="radio" name="rating-3" className="mask mask-star-2 bg-orange-400" />
                <input type="radio" name="rating-3" className="mask mask-star-2 bg-orange-400" />
                <input
                  type="radio"
                  name="rating-3"
                  className="mask mask-star-2 bg-orange-400"
                  defaultChecked
                />
                <input type="radio" name="rating-3" className="mask mask-star-2 bg-orange-400" />
              </div>
              <textarea
                className="textarea textarea-bordered h-24 mt-3"
                placeholder="Deja un comentario sobre tus compañeros..."
              ></textarea>
            </div>

            <div className="form-control mt-8">
              <button className="btn btn-primary w-full shadow-lg">Enviar Valoración</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
