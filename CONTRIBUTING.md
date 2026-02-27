# Guide de Contribution

Merci de votre interet pour contribuer a SenCours !

## Comment contribuer

### Signaler un bug

1. Verifiez que le bug n'a pas deja ete signale
2. Ouvrez une issue avec le template "Bug Report"
3. Decrivez le comportement attendu vs observe
4. Incluez les etapes de reproduction

### Proposer une fonctionnalite

1. Ouvrez une issue avec le template "Feature Request"
2. Decrivez la fonctionnalite et son utilite
3. Attendez la validation avant de coder

### Soumettre du code

1. **Fork** le repository
2. **Clone** votre fork localement
3. Creez une **branche** descriptive :
   ```bash
   git checkout -b feature/ma-nouvelle-fonctionnalite
   ```
4. **Codez** en respectant les conventions
5. **Testez** vos modifications
6. **Commit** avec un message clair :
   ```bash
   git commit -m "feat: ajouter la fonctionnalite X"
   ```
7. **Push** vers votre fork
8. Ouvrez une **Pull Request**

## Conventions de code

### Java

- Suivre les conventions Google Java Style
- Utiliser Lombok pour reduire le boilerplate
- Documenter les methodes publiques avec Javadoc
- Nommer les variables en camelCase
- Nommer les constantes en UPPER_SNAKE_CASE

### Commits

Format : `type: description`

Types :
- `feat` : nouvelle fonctionnalite
- `fix` : correction de bug
- `docs` : documentation
- `style` : formatage
- `refactor` : refactoring
- `test` : ajout de tests
- `chore` : maintenance

### Tests

- Ecrire des tests pour toute nouvelle fonctionnalite
- Maintenir une couverture > 80%
- Utiliser des noms descriptifs pour les tests

## Code de conduite

En participant, vous acceptez de respecter notre [Code de Conduite](CODE_OF_CONDUCT.md).

## Questions ?

N'hesitez pas a ouvrir une issue pour toute question !
