# Storage port

The application talks to object storage through a small **port**. This sample ships an in-memory adapter. A production adapter would implement the same two methods against a bucket. The upload flow does not change.

The port, the two object keys, and the complete rules are in [docs/architecture.md](docs/architecture.md).

1. `POST /api/uploads` records a pending upload and returns a one-time token. The token is stored only as a SHA-256 hash.
2. `PUT /api/uploads/{id}/content` writes the bytes to `staging/{id}` through the port.
3. `POST /api/uploads/{id}/complete` reads staging once, checks the size, and writes `private/{id}`.

Completing an upload that is already `READY` returns `200` and does not read storage again. A size mismatch marks the row `FAILED`. Completing a failed upload returns `400` and does not read storage again.

## Run

Java 21.

```sh
./mvnw spring-boot:run
```

```sh
curl -sS -H 'Content-Type: application/json' \
  -d '{"filename":"note.txt","byteSize":5}' \
  http://127.0.0.1:8080/api/uploads
```

Use the returned `id` and `token`:

```sh
curl -sS -X PUT -H "X-Upload-Token: TOKEN" --data-binary 'hello' \
  http://127.0.0.1:8080/api/uploads/1/content

curl -sS -X POST http://127.0.0.1:8080/api/uploads/1/complete
```

## Tests

```sh
./mvnw test
```

## What this is

A portfolio proof of concept. The in-memory adapter is not a bucket, and the token is not a presigned URL.
