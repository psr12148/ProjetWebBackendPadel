package be.ephec.pdw.projetwebbackendpadel.enums;

public enum StatutParticipation {
    EN_ATTENTE,   // Inscrit mais pas encore payé
    CONFIRME,     // Paiement reçu
    LIBERE        // Place libérée (non-paiement J-1 ou bascule public)
}
