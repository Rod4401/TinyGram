# Nos "Kinds" et nos index

>   **Auteurs:**
>   - Rodrigue    Meunier
>   - Quentin     Gomes Dos Reis
>   - Valentin    Goubon

>   **Formation:** Master 1 ALMA - Nantes Université

Ce document contient tous nos kinds et nos index déployés sur notre DataStore pour notre TinyGram.

## Nos "Kinds"

### Les utilisateurs

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_User.png" alt="Notre liste d'utilisateurs"><br>

Pour chaque entitée "User", on stocke le nom et prénom mais également le pseudo à la première connexion.\
Chaque entitée possède une clé unique relatif au numéro de l'identifiant du compte Google, à chaque connexion, nous mettons à jour l'attribut "lastLogin". Si jamais nous poursuivons le développement de l'appication, ce paramètre nous permettrait de donner les derniers likes ou follows concernant l'user.

### Les posts

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Post.png" alt="Notre liste de Posts"><br>

Pour chaque entitée "Post", on mémorise la description (si elle existe), l'URL du média posté ainsi que la date de création de ce dernier et on mémorise l'ID du créateur.\
Chaque entitée possède une clé unique relatif au numéro de l'identifiant du créateur et l'instant de création.

### Les likes et les compteurs de likes

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Like.png" alt="Notre liste de Likes"><br>

Nous gérons les likes d'une façon spéciale, on ne mémorise pas des listes mais les arêtes qui lient un User à un Post.
Si une arête existe, le Post A a été liké par l'User B sinon ce n'est pas le cas...\
Pour cela, nous avons une entitée nommée "Like" qui est en fait, un triplet composé de l'ID du Post, celui de l'User qui like ainsi que la date auquel cette action a été effectuée.\
Bien sûr, la clé de l'entitée ne dépend que des deux ID et sera unique, un User ne peux liker qu'une seule fois.

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_LikeCounter.png" alt="Notre liste de compteurs de likes"><br>

Ensuite, pour compter tous les likes, nous allons faire une somme de compteurs, c'est-à-dire qu'à la création d'un Post, nous allons créer 10 compteurs (entitée "LikeCounter") qui auront comme parent le Post créé.\
Lorsqu'un User likera un post, nous prendrons en compte la date de réalisation pour savoir quel compteur incrémenter, le compteur incrémenté est le modulo 10 des secondes.\
Cette manipulation, permet en théorie de répartir la charge sur l'intégralité des compteurs.

### Les follows et les compteurs de followers

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_Follow.png" alt="Notre liste de follows"><br>

Nous gérons les follow de la même façon que les likes, on ne mémorise pas des listes mais les arêtes qui lient un User A à un User B.
Si une arête existe, l'User A a suivi l'User B sinon ce n'est pas le cas...\
Pour cela, nous avons une entitée nommée "Follow" qui est en fait, un triplet composé de l'ID du User A, celui de l'User B ainsi que la date auquel cette action a été effectuée.\
Bien sûr, la clé de l'entitée ne dépend que des deux ID et sera unique, un User A ne peux follow l'User B qu'une seule fois.\
On vérifie également que l'User A ne puisse pas follow l'User A, ce qui serait bête.

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/kinds/Kinds_FollowCounter.png" alt="Notre liste compteurs de followers"><br>

Ensuite, pour compter tous les follow, nous allons faire une somme de compteurs, c'est-à-dire qu'à la création d'un User, nous allons créer 10 compteurs (entitée "FollowCounter") qui auront comme parent le User créé.\
Lorsqu'un User A follow un User B, ce sera un des compteurs de l'User B qui sera incrémenté, pour savoir lequel, nous prendrons en compte la date de réalisation pour savoir quel compteur incrémenter, le compteur incrémenté est le modulo 10 des secondes.\
Cette manipulation, permet en théorie de répartir la charge sur l'intégralité des compteurs.

## Nos index

<img src="https://github.com/Rod4401/TinyGram/blob/5d62b7058add12b2ea237a93f94efdd06fbdd8b8/readMeFiles/Index.png" alt="Les index déployés dans notre datastore"><br>

### Index sur les Follow

Sur les entitées Follow, nous avons dépoloyé un index composite sur les trois attributs userID, followedID et date pour pouvoir faire des recherches sur les Users suivis et ceux qui suivent mais également la date auquelle l'action a été effectuée.

### Index sur les Like

Sur les entitées Like, nous avons dépoloyé un index composite sur les trois attributs userID, postID et date pour pouvoir faire des recherches sur les Posts likés et ceux qui ont liké mais également la date auquelle l'action a été effectuée.

### Index sur les Post

Sur les entitées Post, nous avons dépoloyé un index composite sur deux attributs du kind Post, l'ID créateur du Post mais également la date de création.
Ce qui nous permets de rechercher les posts d'un créateur donné.