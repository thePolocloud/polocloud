# Polocloud v3

[![Download](https://img.shields.io/github/downloads/HttpMarco/Polocloud/total?style=for-the-badge&logo=github&color=2ea043)](https://github.com/HttpMarco/polocloud/releases)
[![Discord](https://img.shields.io/discord/1278460874679386244?label=Community&style=for-the-badge&logo=discord&color=7289da)](https://discord.gg/WGzUcuJax7)


<details open="open">
    <summary>Overview</summary>
    <ol>
        <li>
          <a href="#getting-started">Getting Started</a>
          <ul>
            <li><a href="#requirements">Requirements</a></li>
            <li><a href="#installation">Installation</a></li>
          </ul>
        </li>
        <li>
          <a href="#sdk">SDK</a>
          <ul>
            <li><a href="#java">Java</a></li>
            <li><a href="#go">Go</a></li>
          </ul>
        </li>
        <li>
          <a href="#sdk">Running</a>
          <ul>
            <li><a href="#local">Local</a></li>
            <li><a href="#docker">Docker</a></li>
            <li><a href="#k8s">Kubernetes</a></li>
          </ul>
        </li>
    </ol>
</details>

<br/>

### 1. Getting Started

### 2. Using the sdk


### 3. Running

#### 3.1 Local

To run PoloCloud **locally** on your machine, you need:

- A local Java environment (e.g., JDK 17)

**Steps:**

1. Clone the project and build dependencies.
2. Start the application via the start script.

#### 3.2 Docker

You can containerize PoloCloud using Docker to run it in an isolated environment easily.


#### 3.3 Kubernetes

[![Cluster Setup Status](https://img.shields.io/github/actions/workflow/status/httpmarco/polocloud/cluster-setup.yml?label=cluster%20setup)](https://github.com/httpmarco/polocloud/actions/workflows/cluster-setup.yml)

To run PoloCloud in a Kubernetes cluster, you can use the provided initialization script.  
It sets up the required namespace, service accounts, and roles.

```sh
kubectl apply -f https://raw.githubusercontent.com/httpmarco/polocloud/master/scripts/init-cluster.yml
```