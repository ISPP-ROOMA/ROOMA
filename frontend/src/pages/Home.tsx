import { Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function Home() {

    const { token, role } = useAuthStore()

    const getAuthenticatedCTA = () => {
        switch (role) {
            case 'LANDLORD':
                return (
                    <div className="flex flex-wrap gap-4 justify-center">
                        <Link to="/apartments" className="btn btn-primary">Mis Inmuebles</Link>
                        <Link to="/apartments/publish" className="btn btn-ghost">Publicar piso</Link>
                    </div>
                )
            case 'TENANT':
                return (
                    <div className="flex flex-wrap gap-4 justify-center">
                        <Link to="/explore" className="btn btn-primary">Explorar pisos</Link>
                    </div>
                )
            case 'ADMIN':
                return (
                    <div className="flex flex-wrap gap-4 justify-center">
                        <Link to="/users" className="btn btn-primary">Gestionar usuarios</Link>
                    </div>
                )
            default:
                return null
        }
    }

    return (
        <div className="hero min-h-[60vh] bg-base-200">
            <div className="hero-content text-center">
                <div className="max-w-2xl">
                    <h1 className="text-5xl font-bold">Bienvenido a Rooma</h1>
                    <p className="py-6">
                        {!token
                            ? 'Encuentra tu piso ideal o publica el tuyo. Regístrate como inquilino o propietario para empezar.'
                            : role === 'LANDLORD'
                                ? 'Gestiona tus inmuebles y encuentra a los inquilinos perfectos.'
                                : role === 'TENANT'
                                    ? 'Explora pisos disponibles y encuentra tu próximo hogar.'
                                    : 'Panel de administración de Rooma.'
                        }
                    </p>
                    {!token ? (
                        <div className="flex flex-wrap gap-4 justify-center">
                            <Link to="/register" className="btn btn-primary">Crear cuenta</Link>
                            <Link to="/login" className="btn btn-ghost">Iniciar sesión</Link>
                        </div>
                    ) : (
                        getAuthenticatedCTA()
                    )}
                </div>
            </div>
        </div>
    )
}
