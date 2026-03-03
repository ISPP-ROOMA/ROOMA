import { Cloudinary } from '@cloudinary/url-gen'
import { fill } from '@cloudinary/url-gen/actions/resize'
import { AdvancedImage, lazyload, responsive, placeholder } from '@cloudinary/react'

const cld = new Cloudinary({
  cloud: { cloudName: 'djuqshdey' },
})

interface ImageShowProps {
  publicId: string
  alt: string
  className?: string
  width?: number
  height?: number
}

export default function ShowImage({
  publicId,
  alt,
  className = '',
  width = 500,
  height = 500,
}: ImageShowProps) {
  const img = cld
    .image(publicId)
    .format('auto')
    .quality('auto')
    .resize(fill().width(width).height(height))

  return (
    <AdvancedImage
      cldImg={img}
      alt={alt}
      className={className}
      plugins={[lazyload(), responsive({ steps: 200 }), placeholder({ mode: 'blur' })]}
    />
  )
}
