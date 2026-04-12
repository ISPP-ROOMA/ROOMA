import type { ChangeEvent } from 'react'
import { useState, useRef, useMemo, useCallback } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet'
import type { Map as LeafletMap, Marker as LeafletMarker } from 'leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { PublishFormData } from './publishForm'

// Fix missing marker icons in strict bundler environments
delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const WRAPPER_CLASS = 'flex flex-col gap-6'
const MAP_CLASS =
  'relative w-full aspect-[16/10] bg-base-300/60 rounded-2xl flex items-center justify-center overflow-hidden z-0'
const LOCATION_BUTTON_CLASS =
  'flex items-center gap-2 text-primary font-semibold text-sm self-start hover:opacity-80 transition'
const STREET_INPUT_CLASS =
  'input input-bordered w-full pl-11 rounded-xl bg-base-100 focus:outline-primary'

interface Props {
  data: PublishFormData
  updateFields: (fields: Partial<PublishFormData>) => void
}

const DEFAULT_CENTER: [number, number] = [37.3891, -5.9844] // Sevilla

// Component to handle map clicks
function MapEvents({
  setPosition,
  onPositionChanged,
}: {
  setPosition: React.Dispatch<React.SetStateAction<[number, number]>>
  onPositionChanged: (lat: number, lng: number) => void
}) {
  useMapEvents({
    click(e) {
      const { lat, lng } = e.latlng
      setPosition([lat, lng])
      onPositionChanged(lat, lng)
    },
  })
  return null
}

export default function StepLocation({ data, updateFields }: Props) {
  const [position, setPosition] = useState<[number, number]>(DEFAULT_CENTER)
  const mapRef = useRef<LeafletMap>(null)
  const markerRef = useRef<LeafletMarker | null>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Reverse Geocoding (lat, lng -> address)
  const fetchAddressFromCoords = useCallback(async (lat: number, lng: number) => {
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`
      )
      const json = await res.json()
      if (json && json.address) {
        if (json.address.country_code !== 'es') {
          alert('Actualmente solo soportamos apartamentos en España.')
          setPosition(DEFAULT_CENTER)
          if (mapRef.current) mapRef.current.flyTo(DEFAULT_CENTER, 13)
          return
        }

        const road = json.address.road || json.address.pedestrian || ''
        const houseNumber = json.address.house_number || ''
        const city = json.address.city || json.address.town || json.address.village || ''
        const postcode = json.address.postcode || ''
        const suburb = json.address.suburb || json.address.neighbourhood || json.address.city_district || json.address.quarter || ''
        
        let newStreet = road
        if (houseNumber) newStreet += ` ${houseNumber}`
        if (city && !newStreet.includes(city)) {
          // If road is empty but city exists, just use city, else append city
          newStreet = newStreet ? `${newStreet}, ${city}` : city
        }

        if (!newStreet) {
          newStreet = json.display_name.split(',')[0]
        }
        
        const updates: Partial<PublishFormData> = { street: newStreet }
        if (postcode) updates.postalCode = postcode
        if (suburb) updates.neighborhood = suburb

        updateFields(updates)
      }
    } catch (err) {
      console.error('Error in reverse geocoding:', err)
    }
  }, [updateFields])

  // Forward Geocoding (address -> lat, lng)
  const fetchCoordsFromAddress = async (address: string) => {
    if (!address.trim()) return
    try {
      const query = `${address}`
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&countrycodes=es&limit=1`
      )
      const json = await res.json()
      if (json && json.length > 0) {
        const lat = parseFloat(json[0].lat)
        const lon = parseFloat(json[0].lon)
        setPosition([lat, lon])
        if (mapRef.current) {
          mapRef.current.flyTo([lat, lon], 15)
        }
      }
    } catch (err) {
      console.error('Error in forward geocoding:', err)
    }
  }

  const handleStreetChange = (event: ChangeEvent<HTMLInputElement>) => {
    const val = event.target.value
    updateFields({ street: val })

    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      fetchCoordsFromAddress(val)
    }, 1200)
  }

  const handlePostalCodeChange = (event: ChangeEvent<HTMLInputElement>) => {
    const val = event.target.value
    updateFields({ postalCode: val })
    
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      fetchCoordsFromAddress(`${val}`)
    }, 1200)
  }

  const handleNeighborhoodChange = (event: ChangeEvent<HTMLInputElement>) => {
    const val = event.target.value
    updateFields({ neighborhood: val })

    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      const query = [val, data.postalCode].filter(Boolean).join(' ')
      fetchCoordsFromAddress(query)
    }, 1200)
  }

  const useCurrentLocation = () => {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude
          const lng = pos.coords.longitude
          setPosition([lat, lng])
          if (mapRef.current) {
            mapRef.current.flyTo([lat, lng], 16)
          }
          fetchAddressFromCoords(lat, lng)
        },
        (err) => {
          console.error(err)
          alert('No se pudo obtener la ubicación. Verifica los permisos de tu navegador.')
        }
      )
    } else {
      alert('La geolocalización no está soportada en tu navegador.')
    }
  }

  const eventHandlers = useMemo(
    () => ({
      dragend() {
        const marker = markerRef.current
        if (marker != null) {
          const { lat, lng } = marker.getLatLng()
          setPosition([lat, lng])
          fetchAddressFromCoords(lat, lng)
        }
      },
    }),
    [fetchAddressFromCoords]
  )

  return (
    <div className={WRAPPER_CLASS}>
      <div className={MAP_CLASS}>
        <MapContainer
          center={position}
          zoom={13}
          style={{ height: '100%', width: '100%', zIndex: 0 }}
          ref={mapRef}
        >
          <TileLayer
            attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker
            draggable={true}
            position={position}
            ref={markerRef}
            eventHandlers={eventHandlers}
          />
          <MapEvents
            setPosition={setPosition}
            onPositionChanged={(lat, lng) => fetchAddressFromCoords(lat, lng)}
          />
        </MapContainer>
      </div>

      <button type="button" className={LOCATION_BUTTON_CLASS} onClick={useCurrentLocation}>
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

      <div className="flex flex-col md:flex-row gap-4">
        <div className="flex-1">
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
              autoComplete="off"
            />
          </div>
        </div>

        <div className="w-full md:w-32">
          <label className="text-sm font-semibold text-base-content block mb-1.5">
            C. Postal
          </label>
          <input
            type="text"
            value={data.postalCode}
            onChange={handlePostalCodeChange}
            placeholder="Ej. 41001"
            className="input input-bordered w-full rounded-xl bg-base-100 px-3 focus:outline-primary"
            autoComplete="off"
            maxLength={10}
          />
        </div>
      </div>

      <div>
        <label className="text-sm font-semibold text-base-content block mb-1.5">
          Barrio o distrito
        </label>
        <input
          type="text"
          value={data.neighborhood}
          onChange={handleNeighborhoodChange}
          placeholder="Ej. Nervión"
          className="input input-bordered w-full rounded-xl bg-base-100 px-3 focus:outline-primary"
          autoComplete="off"
        />
      </div>
    </div>
  )
}
