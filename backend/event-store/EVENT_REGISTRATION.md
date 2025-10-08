# Event Registration Guide

## Overview

The `EventSerializer` in the event-store module uses **automatic event type registration** via package scanning. This eliminates the need to manually update the serializer when adding new domain events.

## How It Works

### Automatic Registration

When the application starts, the `EventSerializer` scans the `org.haven` package for all classes implementing `DomainEvent` and automatically registers them:

```java
@PostConstruct
void registerEvents() {
    // Scans org.haven package for DomainEvent implementations
    // Automatically maps event.eventType() -> event class
}
```

### Adding New Events

To add a new domain event:

1. **Create your event class** implementing `DomainEvent`:
   ```java
   package org.haven.mymodule.domain.events;

   import org.haven.shared.events.DomainEvent;

   public class MyNewEvent implements DomainEvent {
       @Override
       public String eventType() {
           return "MyNewEvent";
       }
       // ... other methods
   }
   ```

2. **That's it!** The event will be automatically discovered and registered on application startup.

## Requirements for Auto-Registration

For an event to be automatically registered, it must:

1. Implement the `DomainEvent` interface
2. Be located in the `org.haven` package (or subpackages)
3. Either:
   - Have a no-args constructor for instantiation during registration, OR
   - Use the simple class name as the event type (fallback)

## Manual Registration (Optional)

For special cases (testing, external events, runtime registration):

```java
eventSerializer.registerEventType("CustomEventType", CustomEvent.class);
```

## Monitoring Registered Events

To see all registered event types:

```java
Set<String> types = eventSerializer.getRegisteredEventTypes();
logger.info("Registered events: {}", types);
```

The application logs registered event types at startup:
```
INFO  o.h.eventstore.infrastructure.EventSerializer - Scanning for DomainEvent implementations in org.haven package...
INFO  o.h.eventstore.infrastructure.EventSerializer - Registered 15 DomainEvent types
```

## Troubleshooting

### Event Not Found During Deserialization

If you see: `Unknown event type: XYZ. Available types: [...]`

**Causes:**
1. Event class not in `org.haven` package
2. Event class is abstract or an interface
3. Event not on classpath at runtime

**Solutions:**
1. Move event to `org.haven` package
2. Ensure event is a concrete class
3. Check module dependencies in `build.gradle.kts`
4. Manually register the event type if needed

### Performance Considerations

- Package scanning occurs once at application startup via `@PostConstruct`
- Uses `ConcurrentHashMap` for thread-safe event lookup
- Minimal runtime overhead after initialization
- Typical startup time: < 100ms for scanning

## Migration from Hard-Coded Switch

**Before (Hard-coded):**
```java
private String getFullClassName(String eventType) {
    return switch (eventType) {
        case "ConsentGranted" -> "org.haven...ConsentGranted";
        case "ConsentRevoked" -> "org.haven...ConsentRevoked";
        // ... manual entries for each event
    };
}
```

**After (Auto-registration):**
- No code changes needed when adding events
- Events are discovered automatically
- Reduced maintenance burden
- No risk of forgetting to register new events

## Benefits

✓ **Zero maintenance** - No manual registration required
✓ **Fail-fast** - Unknown events throw clear errors with available types
✓ **Discoverable** - Can list all registered events at runtime
✓ **Extensible** - Supports manual registration for edge cases
✓ **Type-safe** - Compile-time checking via `DomainEvent` interface
