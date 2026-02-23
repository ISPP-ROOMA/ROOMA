import { useEffect, useState } from "react";

export interface MatchData{imageUrl:string; userName:string; profilePictureUrl: string; roomTitle: string; price: number}

export const ConfirmMatch = () =>{
    const [isOpen, setIsOpen] = useState<Boolean>(false)
    const [matchData, setMatchData] = useState<MatchData>()

    useEffect(()=>{
        const timer = setTimeout(()=>{setMatchData({
            imageUrl:"/temporal_room_picture.png",
            userName:"Laura M.",
            profilePictureUrl:"/temporal_profile_picture.png",
            roomTitle: "Habitación en Nervión",
            price: 450
        });
        
        setIsOpen(true);
    
        }, 2000);
    
        return() => clearTimeout(timer);
    }, []);

    

    return (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
            <div className="bg-[#FDF8F2] rounded-[2rem] w-full max-w-md p-6 relative shadow-2xl">
                
                <img src={matchData?.imageUrl} alt="Habitación" className="w-full h-60 object-cover rounded-3xl mb-6 shadow-sm" />
                
                <div className="text-center mb-6">
                    <h2 className="text-3xl font-bold text-gray-900 mb-3">¡Match creado!</h2>
                    <p className="text-gray-600 px-4">
                        Buenas Noticias, {matchData?.userName} ha aceptado tu solicitud, ya puedes iniciar una conversación y coordinar una visita.
                    </p>
                </div>
                
                <div className="bg-white rounded-3xl p-4 flex items-center shadow-sm mb-8">
                    <img src={matchData?.profilePictureUrl} alt="foto usuario" className="w-16 h-16 rounded-full object-cover border-2 border-gray-50 shadow-sm" />
                    <div className="flex-1 ml-4">
                        <div className="flex justify-between items-center mb-1">
                            <h3 className="text-lg font-bold text-gray-900">{matchData?.userName}</h3>
                            <span className="bg-green-100 text-green-700 text-[10px] font-bold px-2 py-1 rounded-full tracking-wide">
                                MATCH ACTIVO
                            </span>
                        </div>
                        <p className="text-gray-500 text-sm mb-1">{matchData?.roomTitle}</p>
                        <p className="text-[#0D8282] font-bold text-lg">
                            {matchData?.price} € <span className="text-gray-400 font-normal text-sm">/ mes</span>
                        </p>
                    </div>
                </div>
                
                <div className="flex flex-col gap-3"> {/* Ambos botones funcionaran cuando se creen las pantallas corresponientes*/}
                    <button className="w-full bg-[#0D8282] hover:bg-teal-800 text-white font-bold py-4 rounded-full transition-colors shadow-md">
                        Iniciar chat
                    </button>
                    <button className="w-full text-[#0D8282] font-bold py-4 rounded-full hover:bg-teal-50 transition-colors">
                        Volver a solicitudes
                    </button>
                </div>

            </div>
        </div>            
    );
}