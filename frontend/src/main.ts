import { bootstrapApplication } from '@angular/platform-browser';
// import { provideServerRendering } from '@angular/platform-server';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

console.log('main.ts: Starting bootstrap');

bootstrapApplication(AppComponent, {
  ...appConfig,
  // providers: [
  //   ...appConfig.providers,
  //   provideServerRendering(),
  // ],
})
  .then(() => {
    console.log('main.ts: Bootstrap successful');
  })
  .catch((err) => console.error('main.ts: Bootstrap error', err));
