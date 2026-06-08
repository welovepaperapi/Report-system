# ReportSystem

---


## Architektur-Übersicht

```
reports-parent (pom)
├── api          
├── common       
├── bukkit       
└── bungee      
```

**Abhängigkeiten:** `api` ← `common` ← `bukkit` / `bungee`

**Kommunikation:** Reports werden in MongoDB gespeichert. Bei Erstellung oder Statusänderung sendet das Plugin eine Redis-Pub/Sub-Nachricht an die Kanäle `reports:new` und `reports:status_update`.

---

## Setup-Anleitung

### Voraussetzungen

- Java 21 (oder höher)
- Maven 3.8+
- MongoDB 6.0+
- Redis 6.0+
- Paper 1.21.3 Server (Bukkit) und/oder BungeeCord 1.21 (Proxy)

### MongoDB

1. MongoDB installieren und starten (Standard-Port `27017`)
2. Die Datenbank wird automatisch angelegt (Standard: `reports`)

### Redis

1. Redis installieren und starten (Standard-Port `6379`)
2. Redis wird für die serverübergreifende Echtzeit-Kommunikation benötigt

---

## Konfigurationsbeispiele

### Bukkit (`plugins/ReportSystem/config.yml`)

```yaml
mongo:
  uri: "mongodb://localhost:27017"
  database: "reports"
redis:
  host: "localhost"
  port: 6379
  password: ""
server-name: "server1"
bedrock-prefix: "."
allowed-servers: []
debug: false
```

### BungeeCord (`plugins/ReportSystem/config.yml`)

```yaml
mongo:
  uri: "mongodb://localhost:27017"
  database: "reports"
redis:
  host: "localhost"
  port: 6379
  password: ""
server-name: "bungee-proxy"
bedrock-prefix: "."
```

### Mit Authentifizierung

```yaml
mongo:
  uri: "mongodb://user:passwort@localhost:27017/reports?authSource=admin"
  database: "reports"
redis:
  host: "192.168.1.100"
  port: 6379
  password: "dein-redis-passwort"
```

---

## Build & Deployment

```bash
# Projekt bauen
mvn clean package

# Ausgabe-JARs:
# bukkit/target/reports-bukkit-1.0.0-SNAPSHOT.jar
# bungee/target/reports-bungee-1.0.0-SNAPSHOT.jar
```

1. `reports-bukkit-*.jar` in den `plugins`-Ordner jedes nicht Proxy-Servers kopieren
2. `reports-bungee-*.jar` in den `plugins`-Ordner des BungeeCord-Proxys kopieren
3. Server neustarten
4. `config.yml` anpassen
5. Erneut starten
#
