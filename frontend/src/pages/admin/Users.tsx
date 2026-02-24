import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { getUsers } from "../../service/users.service"
import type { User, UsersResponse } from "../../service/users.service"

export default function Users() {
  const navigate = useNavigate()

  const [users, setUsers] = useState<User[]>([])
  const [page, setPage] = useState<number>(1)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        let currentPage = page
        if (!currentPage) {
          currentPage = 1
        }
        const res = await getUsers(currentPage) as UsersResponse
        setUsers(res.users || [])
        setPage(res.page)
        setTotalPages(res.totalPages)
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    fetchUsers()
  }, [page])

  const prevPage = () => {
    if (page === 1) return
    setPage(page - 1)
  }

  const nextPage = () => {
    if (page === totalPages) return
    setPage(page + 1)
  }

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando usuarios...</p>
  if (!users.length) return <p className="text-center mt-10 text-red-500">No se encontraron usuarios.</p>

  return (
    <div className="p-6 min-h-[70vh]">
      <h1 className="text-3xl font-semibold mb-6">Usuarios</h1>

      <div className="overflow-x-auto">
        <table className="table table-zebra w-full">
          <thead>
            <tr>
              <th>#</th>
              <th>Email</th>
              <th>Rol</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user, index) => (
              <tr key={user.id}>
                <th>{index + 1}</th>
                <td>{user.email}</td>
                <td>
                  <div className={`badge ${user.role === 'ADMIN' ? 'badge-primary' : user.role === 'LANDLORD' ? 'badge-secondary' : 'badge-outline'}`}>{user.role}</div>
                </td>
                <td className="flex gap-2">
                  <button onClick={() => navigate(`/users/${user.id}`)} className="btn btn-sm btn-warning">Editar</button>
                  <button className="btn btn-sm btn-error">Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-center mt-6 gap-4">
        <button className="btn" onClick={prevPage} disabled={page <= 1}>Prev</button>
        <div className="flex items-center px-4">{page}/{totalPages}</div>
        <button className="btn" onClick={nextPage} disabled={page === totalPages}>Next</button>
      </div>
    </div>
  )
}
