import { RenderMode, ServerRoute } from '@angular/ssr';
import { routesIDs } from './routes-ids';

export const serverRoutes: ServerRoute[] = [
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'product/:id',
    renderMode: RenderMode.Prerender,
    async getPrerenderParams() {
      if (!routesIDs || routesIDs.length === 0) {
        throw new Error('No product IDs available at build time');
      }
      return routesIDs.map(id => ({ id }));
    }
  },
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
