# Rest Module

## Configuration `local/modules/Rest-Module`

### Javalin `config.json`

```json
{
  "port": "The port to start on for Javalin"
}
```

Example:
```json
{
  "port": 8080
}
```

### User

//TODO creating a user

# Endpoints:

The following path is always required: <br>
`/api/v1/polocloud/` + a path underneath

- [Login](#POST-login)
- [Node](#GET-nodes)
- [Groups](#GET-groups)
- [Services](#GET-services)


### POST `login/`

Required json fields:
```json
{
  "uuid": "users uuid",
  "password": "password"
}
```

Example:

```json
{
  "uuid": "6f85ec5a-417e-4406-add4-d15f9a3e4f8a",
  "password": "N]xH3rBjS257"
}
```

### GET `nodes/`

Required json fields:
```json
//TODO
```

Example:

```json
//TODO
```

### GET `groups/`

Example Response:

```json
{
  "groups": [
    {
      "proxy": {
        "platform": {
          "proxy": true
        },
        "memory": 512,
        "minOnlineService": 2
      }
    }
  ]
}
```

### GET `services/`

Example Response:

```json
{
  "services": [
    {
      "proxy-1": {
        "group": "proxy",
        "orderedId": 1,
        "state": "ONLINE",
        "id": "acf43786-b755-4eaf-b55e-d9fbb2957d82"
      }
    },
    {
      "proxy-2": {
        "group": "proxy",
        "orderedId": 2,
        "state": "ONLINE",
        "id": "eccb8756-c1e4-451a-97fb-3b254c6156d1"
      }
    }
  ]
}
```