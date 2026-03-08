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
    <div data-theme="light" className="mx-auto min-h-dvh w-full max-w-md bg-[#F5F1E3] text-[#1E293B]">
      <div className="px-5 pt-7 pb-44 sm:px-6">
        <header className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="flex h-8 w-8 items-center justify-center text-[#0F172A]"
            aria-label="Volver"
          >
            <ArrowLeft size={24} strokeWidth={2.5} />
          </button>
          <h1 className="text-[1.65rem] font-extrabold leading-none tracking-[-0.02em] text-[#111827] sm:text-[1.9rem]">
            Contrato Finalizado
          </h1>
        </header>

        <section className="mt-8 flex flex-col items-center text-center sm:mt-10">
          <div className="relative grid h-28 w-28 place-items-center rounded-full bg-[#D6E4DC] sm:h-32 sm:w-32">
            <div className="grid h-20 w-20 place-items-center rounded-full border-[3px] border-[#0C8A80] bg-[#EEF5F1] sm:h-24 sm:w-24">
              <Handshake size={34} className="text-[#0C8A80] sm:h-10 sm:w-10" strokeWidth={2.5} />
            </div>
            <div className="absolute -bottom-1 right-0 grid h-9 w-9 place-items-center rounded-full border-2 border-[#EEF5F1] bg-[#0C8A80] text-white shadow-sm sm:h-10 sm:w-10">
              <Check size={18} strokeWidth={2.75} />
            </div>
          </div>

          <h2 className="mt-8 text-[2rem] font-extrabold leading-[1.08] tracking-[-0.02em] text-[#111827] sm:mt-10 sm:text-[2.4rem]">
            Gracias por tu estancia
          </h2>

          <p className="mt-4 max-w-[320px] text-[1rem] leading-[1.42] text-[#64748B] sm:mt-5 sm:text-[1.1rem]">
            Tu contrato con <span className="font-semibold text-[#1F2937]">{apartmentName}</span>
            {apartmentLocation && <span className="text-[#64748B]"> en {apartmentLocation}</span>} ha
            finalizado. Esperamos que hayas tenido una gran experiencia.
          </p>

          <p className="mt-3 text-[0.95rem] text-[#64748B] sm:text-[1rem]">
            Propietario: <span className="font-semibold text-[#1F2937]">{ownerName}</span>
          </p>
        </section>

        <section className="mt-7 rounded-[24px] bg-[#F7F7F7] px-5 py-6 shadow-[0_2px_0_rgba(15,23,42,0.02)] sm:px-7 sm:py-7">
          <h3 className="text-[1.5rem] font-extrabold leading-none tracking-[-0.02em] text-[#1F2937] sm:text-[1.8rem]">
            Valora tu experiencia
          </h3>

          <p className="mt-4 text-[0.95rem] leading-[1.4] text-[#6B7280] sm:mt-5 sm:text-[1rem]">
            Tu opinion ayuda a futuros inquilinos y mejora la comunidad. Como calificarias tu
            estancia?
          </p>

          <div className="mt-6 flex items-center justify-between px-1 sm:mt-7">
            {Array.from({ length: 5 }).map((_, index) => (
              <Star key={index} size={30} className="text-[#BCC4CF] sm:h-9 sm:w-9" strokeWidth={2.25} />
            ))}
          </div>
        </section>

        <section className="mt-6 flex gap-3 rounded-[22px] border border-[#C8D9D1] bg-[#EAF2ED] px-4 py-4 text-[#0C8A80] sm:mt-8 sm:gap-4 sm:px-6 sm:py-5">
          <div className="mt-1 grid h-8 w-8 flex-none place-items-center rounded-full bg-[#0C8A80] text-white">
            <Info size={18} strokeWidth={2.75} />
          </div>
          <p className="text-[0.85rem] font-semibold leading-[1.35] sm:text-[0.95rem]">
            Politica de resena ciega: tu resena y la del propietario se revelaran solo despues de
            que ambos hayan enviado la suya o pasados 30 dias.
          </p>
        </section>
      </div>

      <footer className="fixed inset-x-0 bottom-[4.75rem] mx-auto w-full max-w-md border-t border-[#E1DED0] bg-[#F5F1E3] px-5 pb-5 pt-4 sm:px-6 md:bottom-0 md:pb-8 md:pt-6">
        <button
          onClick={() => navigate(`/reviews/new/${contractId}/select`)}
          className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-[#0C8A80] text-base font-bold text-white shadow-[0_8px_18px_rgba(12,138,128,0.3)] sm:h-14 sm:text-lg"
        >
          Valorar Ahora
          <SquarePen size={20} strokeWidth={2.75} />
        </button>
        <button
          onClick={() => navigate('/')}
          className="mt-4 block w-full text-center text-base font-semibold text-[#6B7280] sm:mt-5 sm:text-lg"
        >
          Quizas mas tarde
        </button>
      </footer>
    </div>
  )
}
