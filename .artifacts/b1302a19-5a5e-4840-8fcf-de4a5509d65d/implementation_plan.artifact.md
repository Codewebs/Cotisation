# Correction de la remise de cagnotte (Pénalités et Échecs)

L'objectif est de s'assurer que l'activation des pénalités lors d'une remise de cagnotte réduit effectivement le montant net à percevoir. De plus, nous allons nous assurer que les échecs (cotisations impayées) sont correctement gérés et régularisés si l'utilisateur choisit de les déduire.

## User Review Required

> [!IMPORTANT]
> Les pénalités de retard et d'échec seront désormais calculées "à la volée" lors de la préparation de la remise si elles ne sont pas déjà enregistrées dans l'historique du membre pour le cycle en cours. Cela permet de déduire les amendes même si elles n'ont pas été explicitement saisies auparavant.

## Proposed Changes

### [Backend / Data Layer]

#### [MODIFY] [TransactionDao.kt](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/TransactionDao.kt)
- Ajout d'une méthode `aDejaPenaliteTypeCycle` pour vérifier si une pénalité d'un certain type a déjà été appliquée à un membre pour un cycle donné.

### [Logic / ViewModel Layer]

#### [MODIFY] [TransactionViewModel.kt](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/TransactionViewModel.kt)
- Mise à jour de `getCagnotteDetails` : Inclure les pénalités fixes (retard et échec) configurées dans la caisse si le membre est en situation de retard ou d'échec pour le cycle actuel et que la pénalité n'est pas encore enregistrée.
- Mise à jour de `enregistrerCagnotte` :
    - Ajout du paramètre `montantEchecs`.
    - Enregistrement d'une transaction de type `COTISATION` pour régulariser les échecs s'ils ont été déduits de la remise.

### [UI Layer]

#### [MODIFY] [CagnotteValidationScreen.kt](file:///C:/Users/LENOVO/AndroidStudioProjects/Cotisation/app/src/main/java/com/indiza/cotisation/bank/CagnotteValidationScreen.kt)
- Mise à jour de l'appel à `enregistrerCagnotte` pour passer le montant des échecs.
- (Optionnel) Amélioration de la visibilité des déductions dans le récapitulatif.

## Verification Plan

### Automated Tests
- N/A (Tests manuels recommandés sur émulateur)

### Manual Verification
1. Créer une caisse avec une pénalité de retard et d'échec configurée.
2. Faire en sorte qu'un membre soit en retard ou n'ait pas cotisé.
3. Ouvrir l'écran de remise de cagnotte pour ce membre.
4. Cocher "Déduire les pénalités de retard".
5. Vérifier que le "NET À PERCEVOIR" diminue du montant de la pénalité.
6. Valider la remise et vérifier dans le journal des transactions que la pénalité a été enregistrée comme payée.
7. Si "Déduire les échecs" était coché, vérifier que les cotisations manquantes sont désormais marquées comme payées dans l'historique.
