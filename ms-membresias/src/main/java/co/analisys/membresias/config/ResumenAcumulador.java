package co.analisys.membresias.config;

import co.analisys.membresias.messaging.dto.DatoEntrenamientoEvento;

public record ResumenAcumulador(int totalDuracionMinutos, int totalCalorias, int cantidadSesiones) {

    public static ResumenAcumulador vacio() {
        return new ResumenAcumulador(0, 0, 0);
    }

    public ResumenAcumulador acumular(DatoEntrenamientoEvento dato) {
        return new ResumenAcumulador(
                this.totalDuracionMinutos + dato.duracionMinutos(),
                this.totalCalorias + dato.calorias(),
                this.cantidadSesiones + 1);
    }
}
