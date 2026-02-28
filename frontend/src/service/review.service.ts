interface PendingReview {
  contractId: number
  apartmentAddress: string
  endDate: string
}

export const getPendingReviews = async (): Promise<PendingReview[]> => {
  // TODO: Cambiar por la llamada HTTP real cuando el backend (rama de compañero) esté listo
  // Ejemplo de cómo será:
  // const response = await axios.get('/api/users/me/pending-reviews', { headers: { Authorization: `Bearer ${token}` } });
  // return response.data;

  // 🔥 Por ahora, simulamos un retraso de 1.2 segundos para mostrar la experiencia en el frontend
  return new Promise((resolve) => {
    setTimeout(() => {
      // Mock data que representa un contrato finalizado esperando a ser valorado
      resolve([
        {
          contractId: 101,
          apartmentAddress: 'Calle Principal 123',
          endDate: '2026-02-25', // Este será el valor del backend posteriormente
        },
      ])
    }, 1200)
  })
}
