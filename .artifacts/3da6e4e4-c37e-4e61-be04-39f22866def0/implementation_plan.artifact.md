# Correction de la logique de calcul et d'enregistrement de la cagnotte

L'utilisateur a signalé que les déductions (échecs, dettes, pénalités) n'affectent pas correctement le montant à percevoir et que les frais de caisse doivent être déduits du montant final.

## Changements proposés

La logique actuelle soustrait les déductions directement du "Montant Brut à Débiter", ce qui est confus et ne permet pas d'enregistrer le remboursement des dettes. La nouvelle approche séparera le montant débité de la caisse (le jackpot réel) et le montant net perçu par le membre (après toutes les déductions).

### [TransactionViewModel](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/TransactionViewModel.kt)

#### [MODIFY] [TransactionViewModel](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/TransactionViewModel.kt)
- Mettre à jour `enregistrerCagnotte` pour accepter `dettesADeduire` et `penalitesADeduire`.
- Enregistrer des transactions de type `REMBOURSEMENT` et `PENALITE_RETARD` / `PENALITE_ECHEC` si ces montants sont fournis.

### [CagnotteValidationScreen](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/CagnotteValidationScreen.kt)

#### [MODIFY] [CagnotteValidationScreen](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/CagnotteValidationScreen.kt)
- Séparer le calcul du "Montant Brut à Débiter" (Jackpot Théorique - Échecs) et du "Net à Percevoir".
- Mettre à jour l'interface utilisateur pour afficher :
    - Montant Brut (Jackpot total pour les parts choisies).
    - Déduction des échecs (cotisations manquantes).
    - Montant réel de la cagnotte (ce qui sort de la caisse).
    - Déductions optionnelles (Dettes, Pénalités).
    - Frais de caisse (déduits du net).
    - Montant Final Net (ce que le membre reçoit en main propre).

## Plan de vérification

### Vérification Manuelle
- Simuler une remise de cagnotte avec des échecs, des dettes et des pénalités cochées.
- Vérifier que le "Net à percevoir" diminue en conséquence.
- Vérifier après validation que les transactions de remboursement et de frais sont bien enregistrées dans le journal.
