import { Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function Home() {
  const { token } = useAuthStore()

  return (
    <div className="hero min-h-[60vh] bg-base-200">
      <div className="hero-content text-center">
        <div className="max-w-2xl">
          <h1 className="text-5xl font-bold">Bienvenido a Enterprise</h1>
          <p className="py-6">
            Esta es una plataforma interna diseñada para la gestión de usuarios, perfiles y
            administración de tu organización. Inicia sesión o regístrate para comenzar a usar el
            sistema.
          </p>

          {!token && (
            <div className="flex flex-wrap gap-4 justify-center">
              <Link to="/login" className="btn btn-primary">
                Iniciar sesión
              </Link>
              <Link to="/register" className="btn btn-ghost">
                Registrarse
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
