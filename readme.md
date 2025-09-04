## Description
This repo contains a spring boot server that can send and recieve text files. Some key points:
- Accounts and files have an assigned access level. Accounts with too low of an level will not be able to read some documents.
- A relational database is used to store account and file metadata and actual file contents being stored in an object store.
- Uploaded files are encrypted on disk and stored locally. An interface is available to allow different underlying implementations to be programmed (such as to change the encryption or connect to remote object storage).

## Use

A database connection must first be set up. To do so, add an <code>application.properties</code> file inside <code>src/main/resources/</code> with the following contents:
<pre>
  spring.datasource.url=jdbc:[your database url]
  spring.datasource.username=[your database username]
  spring.datasource.password=[your database password]
</pre>

The database should also be set up with tables for accounts and files (exact columns names are describe inside the models within <code>models/</code> ).

There are also some example secret keys that should probably be changed in the files: <code>services/Auth.java</code> and <code>services/FileStorageService.java</code>.

To start the server run the command:
<pre>
  # if you're on windows
  ./gradlew run
</pre>

Communications are done through a REST API. Examples:

<pre>
  # logging in
  curl -X POST http://localhost:8080/account/login  ^
  -H "Content-Type: application/json"  ^
  -d "{\"username\":\"username\",\"password\":\"password\"}"
  
  # returns
  {"userId":1,"sessionToken":[a long session token string]}

  
  # viewing all files
  curl -i -X GET http://localhost:8080/file  ^
  -H "Authorization: [session token here]"
  
  # returns
  {"files":"[{ id: 2, type: text, name: test.txt, accessLevel: 2 }]"}
</pre>
