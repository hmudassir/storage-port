# Architecture

Upload logic depends on a storage port. The in-memory adapter is one implementation. A bucket adapter would provide the same two methods.

## Boundaries

```text
UploadController
        |
        v
UploadService
  create   pending row, token returned once, hash stored
  store    write staging/{id} through the port
  complete read staging once, write private/{id}, mark READY
        |
        v
ObjectStorage
  put(key, bytes)
  get(key)
        |
        v
InMemoryObjectStorage          this sample's adapter
```

The service chooses the keys. The caller does not name the object. Staging and the private object are different keys, so a later upload to staging does not overwrite the private object after complete.

## Request path

1. `POST /api/uploads` records the filename, the declared size, and the SHA-256 of a one-time token. The response returns the raw token.
2. `PUT /api/uploads/{id}/content` checks the token and writes the body to `staging/{id}`.
3. `POST /api/uploads/{id}/complete` reads staging once, checks the length against the declared size, writes `private/{id}`, and sets `READY`.

A second complete of a `READY` upload returns `200` and does not call `get`. The private bytes are already accepted.

## Failure

| Condition | Result |
| --- | --- |
| Wrong upload token | `400`, staging is not written |
| Declared size and body length differ | status `FAILED`, `400` |
| Complete of a `FAILED` upload | `400`, storage is not read again |
| Missing upload id | `404` |

The failed status is committed on purpose. The rejection is the outcome of the check, so a retry must see `FAILED` and stop reading staging.

## What this sample leaves out

The token is not a presigned URL, and the adapter is not a bucket. Replacing `InMemoryObjectStorage` with an adapter that `put`s and `get`s the same keys would keep this flow. Expiry of the upload token, and a read URL for the private key, would sit in the service beside the port.
