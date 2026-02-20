import { NavLink } from "react-router-dom"

export default function Footer() {
  return (
    <footer className="footer p-10 bg-base-200 text-base-content mt-10">
      <div className="container mx-auto">
        <div className="flex flex-col md:flex-row justify-between items-start gap-6">
          <div>
            <h2 className="text-xl font-semibold">Enterprise</h2>
            <p className="text-sm mt-1">© {new Date().getFullYear()} Todos los derechos reservados.</p>
          </div>

          <div className="flex flex-wrap gap-4 text-sm">
            <NavLink to="/terms" className="link link-hover">Terms and conditions</NavLink>
            <NavLink to="/contact" className="link link-hover">Contact</NavLink>
            <NavLink to="/about" className="link link-hover">About us</NavLink>
          </div>
        </div>
      </div>
    </footer>
  )
}
