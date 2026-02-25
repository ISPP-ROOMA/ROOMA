import { Cloudinary } from '@cloudinary/url-gen';
import { AdvancedImage, lazyload, responsive, placeholder } from '@cloudinary/react';

const cld = new Cloudinary({ 
  cloud: { cloudName: 'djuqshdey' }
});

interface ImageShowProps {
  publicId: string;
  alt: string;
  className?: string;
  width?: number;
  height?: number;
}

export default function ShowImage({ 
  publicId, 
  alt, 
  className = '',
  width = 500,
  height = 500 
}: ImageShowProps) {
  const img = cld
    .image(publicId)
    .format('auto')
    .quality('auto')
    .resize(`w_${width},h_${height},c_fill`);

  return (
    <AdvancedImage 
      cldImg={img} 
      alt={alt} 
      className={className} 
      plugins={[lazyload(), responsive(200), placeholder('blur')]} 
    />
  );
}