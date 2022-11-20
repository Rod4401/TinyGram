# Analyse de la scalabilité

>   **Auteurs:**\
>   - Rodrigue    Meunier
>   - Quentin     Gomes Dos Reis
>   - Valentin    Goubon

>   **Formation:** Master 1 ALMA - Nantes Université

Ceci est notre rapport concernant la scalabilité de notre TinyGram.

## Avant-propos 
Afin de réaliser ce rapport, nous avons utilisé les journaux de notre projet ainsi que d'autres présent sur Google Cloud Platform pour vérifier les temps de réponses moyens de notre TinyGram.  Nous avons également vérifié l'absence d'incohérence entre les valeurs offertes par GCP et celles fournies dans la console de navigateur.\
Et pour obtenir un plus large échantillon de données pour réaliser nos latences, nous avons invité une multitudes de nos connaissances à essayer notre TinyGram.

## Latence moyenne du site complet
Dans un premier temps, nous allons commencer par observer la latence moyenne de notre site, ressources internes et API REST inclues.

<img src="https://github.com/Rod4401/TinyGram/blob/5c98006661bd5c498d76654ffe830e2dac4db05e/src/main/webapp/img/courbeDensite%CC%81.png" alt="Courbe de densité des latences globales"><br>

##### Courbe de densité des latences globales

Sur cette courbe, on peux directement remarquer plusieurs pics après 900ms, nous avons plusiseurs explications à cela, la première étant l'impact du délais de provision d'une machine GCP pour répondre à la requête.\
La seconde raison serait un temps de latence élevé dû aux nombreux types de connexions internet utilisées par nos testeurs ainsi que les différentes localisations de ces derniers.

<img src="https://github.com/Rod4401/TinyGram/blob/5c98006661bd5c498d76654ffe830e2dac4db05e/src/main/webapp/img/courbeCumule%CC%81eCroissante.png" alt="Courbe cumulée croissante des latences globales"><br>

##### Courbe cumulée croissante des latences globales

Cette courbe, nous permet de voir que la majorité des latences de notre site restent raisonnables puisque pour la plupart, elles rentrent dans les exigences, c'est-à-dire, autour de 400-500 ms.\
On remarque également une nette augmentation après 900ms dont les raisons de sa présence sont les mêmes que celles évoquées précédemment.


## Nos benchmarks

### Avant-propos 

Dans le cadre de ce projet, il nous a été demandé de réaliser des benchmarks sur notre TinyGram et plus particulièrement sur l'API REST de ce dernier.\
Bien sûr, nous ne prendrons pas en compte le délai de provisionnement de la machine GCP qui fausseront nos mesures, nous partirons du principe qu'une instance GCP est déjà prête et démarée.


### Combien de temps pour poster un post ?

Sur une cinquantaine de requêtes, nous avons mesuré un temps de réponse compris entre 300ms et 550ms pour poster un post.\
Donc théoriquement, un utilisateur pourrait poster entre 1 et 3 post par seconde, ce qui est irréalisable par un humain (dans des conditions strictement normales).\
Et avec notre conception, nous ne sommes pas limités par le nombre de followers, c'est-à-dire que le nouvel utilisateur mettera autant de temps à poster un post qu'un  utilisateur à plusieurs milliers de followers.

### Combien de temps pour récupérer des posts ?

Sur environ 700 requêtes, nous avons mesuré un temps de réponse compris entre 50ms et 200ms pour récupérer 10 posts (avec ou sans curseur).<br>
Donc théoriquement, pour récupérer 100 posts, il faudrait 15 secondes mais étant donné qu'on charge les posts par blocs de 10, il faudrait effectuer 10 requêtes tout en veillant à bien utiliser le curseur.\
Maintenant pour récupérer 500 messages, il faudrait 1 minute et 15 secondes et effectuer 50 requêtes pour arriver à récupérer un tel nombre de posts.\
Bien sûr, les temps donnés précédemment ne prennent pas en compte les délais coté client.

### Combien de likes par seconde ?

Sur une cinquantaine de requêtes, nous avons mesuré un temps de réponse compris entre 140ms et 250ms pour liker un post.<br>
Il semble envisageable de penser qu'un utilisateur pourrait liker entre 4 et 7 fois par seconde.