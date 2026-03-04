import { ArrowLeft, Check, Handshake, Info, SquarePen, Star } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { type ApartmentInfo, getApartmentInfo } from '../../service/review.service'

export default function ReviewContractFinished() {
  const navigate = useNavigate()
  const { contractId } = useParams()
  const apartmentId = Number(contractId)

  const [apartment, setApartment] = useState<ApartmentInfo | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!apartmentId) return
    getApartmentInfo(apartmentId)
      .then((data) => setApartment(data))
      .finally(() => setLoading(false))
  }, [apartmentId])

  if (loading) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-[#F5F1E3]">
        <span className="loading loading-dots loading-lg text-[#0C8A80]"></span>
      </div>
    )
  }

  const apartmentName = apartment?.title ?? 'tu piso'
  const apartmentLocation = apartment?.ubication ?? ''
  const ownerEmail = apartment?.ownerEmail ?? 'el propietario'
  const ownerName = ownerEmail.split('@')[0]

  return (
    <div
      data-theme="light"
      className="mx-auto min-h-dvh w-full max-w-md bg-[#F5F1E3] text-[#1E293B]"
    >
      <div className="px-6 pt-8 pb-44">
        <header className="flex items-center gap-3">
          <button
            onClick={() => {
              navigate(-1)
            }}
            className="flex h-8 w-8 items-center justify-center text-[#0F172A]"
            aria-label="Volver"
          >
            <ArrowLeft size={24} strokeWidth={2.5} />
          </button>
          <h1 className="text-[2rem] font-extrabold leading-none tracking-[-0.02em] text-[#111827]">
            Contrato Finalizado
          </h1>
        </header>

        <section className="mt-12 flex flex-col items-center text-center">
          <div className="relative grid h-32 w-32 place-items-center rounded-full bg-[#D6E4DC]">
            <div className="grid h-24 w-24 place-items-center rounded-full border-[3px] border-[#0C8A80] bg-[#EEF5F1]">
              <Handshake size={40} className="text-[#0C8A80]" strokeWidth={2.5} />
            </div>
            <div className="absolute -bottom-1 right-0 grid h-10 w-10 place-items-center rounded-full border-2 border-[#EEF5F1] bg-[#0C8A80] text-white shadow-sm">
              <Check size={20} strokeWidth={2.75} />
            </div>
          </div>

          <h2 className="mt-11 text-[2.9rem] font-extrabold leading-[1.05] tracking-[-0.02em] text-[#111827]">
            ¡Gracias por tu estancia!
          </h2>

          <p className="mt-5 max-w-[320px] text-[1.28rem] leading-[1.38] text-[#64748B]">
            Tu contrato con <span className="text-[#1F2937] font-semibold">{apartmentName}</span>
            {apartmentLocation && (
              <span className="text-[#64748B]"> en {apartmentLocation}</span>
            )}{' '}
            ha finalizado. Esperamos que hayas tenido una gran experiencia.
          </p>

          {ownerName && (
            <p className="mt-3 text-[1.1rem] text-[#64748B]">
              Propietario: <span className="font-semibold text-[#1F2937]">{ownerName}</span>
            </p>
          )}
        </section>

        <section className="mt-8 rounded-[30px] bg-[#F7F7F7] px-7 py-8 shadow-[0_2px_0_rgba(15,23,42,0.02)]">
          <h3 className="text-[2.15rem] font-extrabold leading-none tracking-[-0.02em] text-[#1F2937]">
            Valora tu experiencia
          </h3>

          <p className="mt-6 text-[1.2rem] leading-[1.35] text-[#6B7280]">
            Tu opinión ayuda a futuros inquilinos y mejora la comunidad. ¿Cómo calificarías tu
            estancia?
          </p>

          <div className="mt-8 flex items-center justify-between px-1">
            {Array.from({ length: 5 }).map((_, index) => (
              <Star key={index} size={36} className="text-[#BCC4CF]" strokeWidth={2.25} />
            ))}
          </div>
        </section>

        <section className="mt-9 flex gap-4 rounded-[26px] border border-[#C8D9D1] bg-[#EAF2ED] px-6 py-5 text-[#0C8A80]">
          <div className="mt-1 grid h-8 w-8 flex-none place-items-center rounded-full bg-[#0C8A80] text-white">
            <Info size={18} strokeWidth={2.75} />
          </div>
          <p className="text-[1.03rem] font-semibold leading-[1.3]">
            Política de Reseña Ciega: Tu reseña y la del propietario se revelarán solo después de
            que ambos hayan enviado la suya o pasados 30 días.
          </p>
        </section>
      </div>

      <footer className="fixed bottom-0 left-0 right-0 mx-auto w-full max-w-md border-t border-[#E1DED0] bg-[#F5F1E3] px-6 pb-8 pt-6">
        <button
          onClick={() => {
            navigate(`/reviews/new/${contractId}/select`)
          }}
          className="flex h-16 w-full items-center justify-center gap-3 rounded-[20px] bg-[#0C8A80] text-[1.95rem] font-extrabold text-white shadow-[0_8px_18px_rgba(12,138,128,0.3)]"
        >
          Valorar Ahora
          <SquarePen size={25} strokeWidth={2.75} />
        </button>
        <button
          onClick={() => {
            navigate('/')
          }}
          className="mt-7 block w-full text-center text-[1.95rem] font-semibold text-[#6B7280]"
        >
          Quizás más tarde
        </button>
      </footer>
    </div>
  )
}
