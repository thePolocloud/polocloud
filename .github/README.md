### Polocloud


cloud group optional properties 

### Global cluster properties

| id                | description                            | type   | default value    | implemented |
|-------------------|----------------------------------------|--------|------------------|-------------|
| prompt            | The input prefix of the cloud terminal | Text   | '&3cloud &2» &1' | yes         |
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
