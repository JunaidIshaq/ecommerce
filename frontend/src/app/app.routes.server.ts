import {RenderMode, ServerRoute} from '@angular/ssr';
import {routesIDs} from './routes-ids';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'product/:id',
    renderMode: RenderMode.Prerender,
    async getPrerenderParams() {
      return routesIDs.map(id => ({ id }));
    },
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
