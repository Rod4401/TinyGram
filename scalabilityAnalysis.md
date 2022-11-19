# Analyse de la scalabilité

Ceci est notre rapport concernant la scalabilité de notre TinyGram.

## Avant-propos 
Afin de réaliser ce rapport, nous avons utilisé les journaux de notre projet ainsi que d'autres présent sur Google Cloud Platform pour vérifier les temps de réponses moyens de notre TinyGram.  Nous avons également vérifié l'absence d'incohérences entre les valeurs offertes par GCP et celles fournies dans la console de navigateur.
Et pour obtenir un plus large échantillon de données pour réaliser nos latences, nous avons invité une multitudes de nos connaissances à essayer notre TinyGram.

## Latence moyenne du site complet
Pour commencer, nous allons commençons par observer la latence moyenne de notre site, ressources internes et API REST inclues.

<img src="https://github.com/Rod4401/TinyGram/blob/5c98006661bd5c498d76654ffe830e2dac4db05e/src/main/webapp/img/courbeDensite%CC%81.png" alt="Courbe de densité des latences globales"><br>

##### Courbe de densité des latences globales

Sur cette courbe, on peux directement remarquer plusieurs pics après 900ms, nous avons plusiseurs explications à cela, la première étant l'impact du délais de provision d'une machine GCP pour répondre à la requête.\
La seconde raison serait un temps de latence élevé dû aux nombreux types de connections internet utilisées par nos testeurs ainsi que les différentes localitées de ces derniers.

<img src="https://github.com/Rod4401/TinyGram/blob/5c98006661bd5c498d76654ffe830e2dac4db05e/src/main/webapp/img/courbeCumule%CC%81eCroissante.png" alt="Courbe cumulée croissante des latences globales"><br>

##### Courbe cumulée croissante des latences globales

Cette courbe, nous permet de voir que la majorité des latences de notre site restent raisonnables puisque la plupart de ces dernières tournent autour de 400-500 ms.\
On remarque également une nette augmentation après 900ms dont les raisons de sa présence sont les mêmes que celles évoquées précédemment.


## Nos benchmarks

### Avant-propos 

Dans le cadre de ce projet, il nous a été demandé de réaliser des benchmarks sur notre TinyGram et plus particulièrement sur l'API REST de ce dernier.\
Bien sûr, nous ne prendrons pas en compte le délai de provisionnement de la machine GCP qui fausseront nos mesures, nous partirons du principe qu'une instance GCP est déjà prête et démarée.


### Combien de temps pour poster un post ?

Sur une cinquantaine de requêtes, nous avons mesuré un temps de réponse compris entre 300ms et 550ms pour poster un post. \

Donc théoriquement, un utilisateur pourrait poster entre 1 et 3 post par seconde, ce qui est irréalisable par un humain...\

Et avec notre conception, nous ne sommes pas limités par le nombre de followers, c'est-à-dire que le nouvel utilisateur mettera temps à poster un post qu'un utilisateur à plusieurs milliers de followers.

### Combien de temps pour récupérer des posts ?

Sur environ 700 requêtes, nous avons mesuré un temps de réponse compris entre 50ms et 200ms pour récupérer 10 posts (avec ou sans curseur).\

Donc théoriquement, pour récupérer 100 posts, il faudrait 15 secondes mais étant donné qu'on charge les posts par blocs de 10, il faudrait effectuer 10 requêtes tout en veillant à bien utiliser le curseur.\

Maintenant pour récupérer 500 messages, il faudrait 1 minute et 15 secondes et effectuer 50 requêtes pour arriver à récupérer un tel nombre de posts.\

Bien sûr, les temps donnés précédemment ne prennent pas en compte les délais incombants au client.

### Combien de likes par seconde ?

Sur une cinquantaine de requêtes, nous avons mesuré un temps de réponse compris entre 140ms et 250ms pour liker un post.\
Donc théoriquement, un utilisateur pourrait liker entre 4 et 7 fois par seconde.


## Nos "Kinds"

### Les utilisateurs

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_User.png" alt="Notre liste d'utilisateurs"><br>

Pour chaque entitée "User", on stocke les nom et prénom mais également le pseudo à la première connection.\
Chaque entitée possède une clé unique relatif au numéro de l'identifiant du compte Google, à chaque connection, nous mettons à jour l'attribut "lastLogin", si on poursuit le développement de TinyGram, ce paramètre nous permettrait de donner les derniers likes ou follows concernant l'user.

### Les posts

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Post.png" alt="Notre liste de Posts"><br>

Pour chaque entitée "Post", on stocke la description (si elle existe), l'URL du média posté ainsi que la date de création de ce dernier et on stocke l'ID du créateur.\
Chaque entitée possède une clé unique relatif au numéro de l'identifiant du créateur et l'instant de création.

### Les likes et les compteurs de likes

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Like.png" alt="Notre liste de Likes"><br>

Nous gérons les likes d'une façon spéciale, on ne stocke pas des listes mais les arêtes qui lient un User à un Post.
Si une arête existe, le Post A à été liké par l'User B sinon ce n'est pas le cas...
Pour cela, nous avons une entitée nommée "Like" qui est en fait, un triplet composé de l'ID du Post, celui de l'User qui like ainsi que la date auquel cette action a été effectuée.
Bien sûr, la clé de l'entitée ne dépend que des deux ID et sera unique, un User ne peux liker qu'une seule fois.

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_LikeCounter.png" alt="Notre liste de compteurs de likes"><br>

Ensuite, pour compter tous les likes, nous allons plutôt faire une somme de compteurs, c'est à dire qu'à la création d'un Post, nous allons créer 10 compteurs (entité "LikeCounter") qui auront comme parent le Post créé.
Lorsqu'un User likera un post, nous prendrons en compte la date de réalisation pour savoir quel compteur incrémenter, le compteur incrémenté est le modulo 10 des secondes.
Cette manipulation, permet en théorie de répartir la charge sur l'intégralité des compteurs.

### Les follows et les compteurs de followers

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Follow.png" alt="Notre liste de follows"><br>

Nous gérons les follow de la même façon que les likes, on ne stocke pas des listes mais les arêtes qui lient un User A à un User B.
Si une arête existe, l'User A a suivi l'User B sinon ce n'est pas le cas...
Pour cela, nous avons une entitée nommée "Follow" qui est en fait, un triplet composé de l'ID du User A, celui de l'User B ainsi que la date auquel cette action a été effectuée.
Bien sûr, la clé de l'entitée ne dépend que des deux ID et sera unique, un User A ne peux follow l'User B qu'une seule fois.
On vérifie également que l'User A ne puisse pas follow l'User A, ce qui serait bête.

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_FollowCounter.png" alt="Notre liste compteurs de followers"><br>

Ensuite, pour compter tous les follow, nous allons plutôt faire une somme de compteurs, c'est à dire qu'à la création d'un User, nous allons créer 10 compteurs (entité "FollowCounter") qui auront comme parent le User créé.
Lorsqu'un User A follow un User B, ce sera un des compteurs de l'User B qui sera incrémenté, pour savoir lequel, nous prendrons en compte la date de réalisation pour savoir quel compteur incrémenter, le compteur incrémenté est le modulo 10 des secondes.
Cette manipulation, permet en théorie de répartir la charge sur l'intégralité des compteurs.

## Nos index

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/Index.png" alt="Les index déployés dans notre datastore"><br>
