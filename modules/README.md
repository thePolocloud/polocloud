# Module System

To create a module, simply implement the `CloudModule` class.

## Example
```java
public class TestModule implements CloudModule {
    
    @Override
    public void onEnable() {
        
    }

    @Override
    public void onDisable() {
        
    }
}
```

## Configuration
To enable your module, create a `module.json` file in your resources folder with the following parameters:
```json
{
  "name": "The name of your module",
  "author": "Your name",
  "main": "The main class of your module"
}
```

### Example

```json
{
  "name": "REST API",
  "author": "RECHERGG",
  "main": "dev.httpmarco.polocloud.rest.RestAPI"
}
```

Place the built module in the `local/modules` directory and start Polocloud.<br>
Enjoy your building your Module 🤩

//TODO create commands<br>
//TODO use events