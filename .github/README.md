# PoloCloud

## Installation

To install PoloCloud, follow these steps:

1. Download the latest release from the [releases page](https://github.com/HttpMarco/polocloud/releases/).
2. Go to the folder in which the downloaded jar is located and execute the following command there:
```bash
  java -jar polocloud.jar
```
3. Create a Proxy and Lobby Group using 'group create'
4. Find the 'Lobby.json' file in the 'local/groups' folder and add the 'fallback' property into it
5. Connect to your Server using ip-address:DEFAULT-PROXY-PORT 

### Global cluster properties

| id                | description                            | type   | default value    | implemented |
|-------------------|----------------------------------------|--------|------------------|-------------|
| prompt            | The input prefix of the cloud terminal | Text   | '&3cloud &2» &1' | yes         |s
| server-port-range | The detection range of proxy ports     | Number | 30000            | yes         |
| proxy-port-range  | The detection range of server ports    | Number | 25565            | yes         |


### Cloud group properties
| id                               | description | type              | default value  | implemented |
|----------------------------------|-------------|-------------------|----------------|-------------|
| static                           |             | State             | false          | yes         |
| maxOnlineServices                |             | Number            | -1             | yes         |
| startArguments                   |             | Text              | ''             | //todo      |
| percentageToStartNewService      |             | Percentage Number | 100.0          | //todo      |
| preferredFallback                |             | Text list         | ''             | //todo      |
| fallback                         |             | State             | false          | yes         |
| startPriority                    |             | Number            | 0              | //todo      |
| mergedTemplates                  |             | Text list         | []             | //todo      |
| environmentVariables             |             | Text list         | []             | //todo      |
| name-separator                   |             | Text              | '-'            | //todo      |
| restartOnTemplateChange          |             | State             | false          | //todo      |
| autoFileDeleteOnShutdown         |             | State             | false          | //todo      |
| portRange                        |             | Number            | -1             | //todo      |
| disablePlatformCache             |             | State             | false          | //todo      |
| disableConfigurationManipulation |             | State             | false          | //todo      |
| maintenance                      |             | State             | false          | yes         |
 | DEBUG_MODE                      |             | State             | false          | yes         | 

### Template properties
| id               | description | type        | default value | implemented |
|------------------|-------------|-------------|---------------|-------------|
| expire           |             | Date        | -1            | //todo      |
| serverPrediction |             | String list | []            | //todo      |
