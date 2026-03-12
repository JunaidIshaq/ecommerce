# Venzora

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.1.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.


Frontend Deployment :

sudo docker builder prune -af
sudo docker volume prune -f


deletes stopped containers, images, and volumes if you confirm with y.
docker system prune -a

docker stop frontend && docker rm frontend
docker build --no-cache -t ecommerce-frontend .
docker run -d --name frontend -p 3000:80 ecommerce-frontend
docker run -d --name mongodb -p 27017:27017 mongo

################# ShopFast Frontend ###################

cd ecommerce/frontend
sudo rm -rf dist
git pull origin master
npm run build -- --configuration production
sudo rm -rf /var/www/shopfast.live/*
sudo cp -r dist/frontend/browser/* /var/www/shopfast.live/

######################################################

Backend Deployment :

Remove all containers :


cd
cd ecommerce/backend/product-service/

docker compose up -d --build

sudo docker compose down
sudo docker compose build
sudo docker compose up -d

sudo docker compose down
sudo docker compose build --no-cache
sudo docker compose up -d

git pull origin master
sudo docker compose down product-service
sudo docker compose build --no-cache product-service
sudo docker compose up -d

git pull origin master
sudo docker compose down category-service
sudo docker compose build --no-cache category-service
sudo docker compose up -d

git pull origin master
sudo docker compose down inventory-service
sudo docker compose build --no-cache inventory-service
sudo docker compose up -d

git pull origin master
sudo docker compose down order-service
sudo docker compose build --no-cache order-service
sudo docker compose up -d

git pull origin master
sudo docker compose down payment-service
sudo docker compose build --no-cache payment-service
sudo docker compose up -d

git pull origin master
sudo docker compose down user-service
sudo docker compose build --no-cache user-service
sudo docker compose up -d

git pull origin master
sudo docker compose down user-service auth-service
sudo docker compose build --no-cache auth-service user-service
sudo docker compose up -d

git pull origin master
sudo docker compose down eureka-server
sudo docker compose build --no-cache eureka-server
sudo docker compose up -d

git pull origin master
sudo docker compose down api-gateway
sudo docker compose build --no-cache api-gateway
sudo docker compose up -d

git pull origin master
sudo docker compose down kafka-ui
sudo docker compose build --no-cache kafka-ui
sudo docker compose up -d

git pull origin master
sudo docker compose down elastic-service
sudo docker compose build --no-cache elastic-service
sudo docker compose up -d

git pull origin master
sudo docker compose down auth-service
sudo docker compose build --no-cache auth-service
sudo docker compose up -d

git pull origin master
sudo docker compose down admin-service
sudo docker compose build --no-cache admin-service
sudo docker compose up -d

Find logs of container
docker logs product-service

sudo systemctl start mongod.service
sudo systemctl start elasticsearch.service
sudo systemctl start kibana.service
sudo systemctl start redis.server

sudo systemctl stop mongod.service
sudo systemctl stop elasticsearch.service
sudo systemctl stop kibana.service
sudo systemctl stop redis.server

sudo systemctl disable mongod.service
sudo systemctl disable elasticsearch.service
sudo systemctl disable kibana.service
sudo systemctl disable redis.server

sudo tail -n 50 /var/log/mongodb/mongod.log

Reload Nginx
sudo nginx -t
sudo systemctl restart nginx.service

############### REDIS ######################

docker exec -it redis redis-cli
keys *

Invalidate Redis Cache
docker exec -it redis redis-cli FLUSHALL

Local :
redis-cli
FLUSHALLsudo docker compose up -d

git pull origin master
sudo docker compose down cart-service
sudo docker compose build --no-cache cart-service
sudo docker compose up -d

git pull origin master
sudo docker compose down notification-service
sudo docker compose build --no-cache notification-service
