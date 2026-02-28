import { useRef } from 'react'

const WRAPPER_CLASS = 'flex flex-col py-6 gap-5'
const PICKER_CLASS =
  'w-full border-2 border-dashed border-base-300 rounded-2xl p-6 text-center hover:border-primary/60 transition'
const GRID_CLASS = 'grid grid-cols-2 sm:grid-cols-3 gap-3'
const THUMB_CLASS = 'relative rounded-xl overflow-hidden bg-base-200 aspect-square'

interface StepPhotosProps {
  images: File[]
  onChangeImages: (images: File[]) => void
  maxImages?: number
}

export default function StepPhotos({ images, onChangeImages, maxImages = 10 }: StepPhotosProps) {
  const inputRef = useRef<HTMLInputElement | null>(null)

  const handlePickFiles = (event: React.ChangeEvent<HTMLInputElement>) => {
    const fileList = event.target.files
    if (!fileList) return

    const picked = Array.from(fileList).filter((f) => f.type.startsWith('image/'))
    const next = [...images, ...picked].slice(0, maxImages)
    onChangeImages(next)

    // permite re-seleccionar el mismo archivo
    event.target.value = ''
  }

  const removeImage = (index: number) => {
    onChangeImages(images.filter((_, i) => i !== index))
  }

  return (
    <div className={WRAPPER_CLASS}>
      <div className={PICKER_CLASS}>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple
          className="hidden"
          onChange={handlePickFiles}
        />

        <button type="button" className="btn btn-primary rounded-full" onClick={() => inputRef.current?.click()}>
          Seleccionar fotos
        </button>

        <p className="text-sm text-base-content/60 mt-3">
          JPG/PNG/WebP · Máximo {maxImages} imágenes
        </p>
      </div>

      {images.length > 0 && (
        <div className={GRID_CLASS}>
          {images.map((file, index) => (
            <div key={`${file.name}-${index}`} className={THUMB_CLASS}>
              <img
                src={URL.createObjectURL(file)}
                alt={file.name}
                className="w-full h-full object-cover"
              />
              <button
                type="button"
                className="btn btn-xs btn-circle absolute top-2 right-2"
                onClick={() => removeImage(index)}
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}