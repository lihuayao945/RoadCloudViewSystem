import { createRouter, createWebHistory } from 'vue-router'
import IndexView from '../views/IndexView.vue'
import LoginView from '../views/LoginView.vue'
import DetailView from '../views/DetailView.vue'
import AdminView from '../views/AdminView.vue'
import UsersView from '../views/admin/user/UsersView.vue'
import RolesView from '../views/admin/user/RolesView.vue'
import PermissionsView from '../views/admin/PermissionsView.vue'
import DataView from '../views/admin/DataView.vue'
import VehiclesView from '../views/admin/data/vehicles/index.vue'
import VehicleDetailView from '../views/admin/data/vehicles/VehicleDetailView.vue'
import RcuObjectsView from '../views/admin/data/RcuObjectsView.vue'
import SystemView from '../views/admin/SystemView.vue'
import ParametersView from '../views/admin/system/ParametersView.vue'
import TopicsView from '../views/admin/system/TopicsView.vue'
import LogsView from '../views/admin/system/LogsView.vue'
import ProfileView from '../views/admin/ProfileView.vue'
import Scene3DView from '../views/Scene3DView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/login'
    },
    {
      path: '/index',
      name: 'index',
      component: IndexView,
      meta: { showNav: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { showNav: false }
    },
    {
      path: '/detail',
      name: 'detail',
      component: DetailView,
      meta: { showNav: true }
    },
    {
      path: '/scene3d',
      name: 'scene3d',
      component: Scene3DView,
      meta: { showNav: false }
    },
    {
      path: '/admin',
      component: AdminView,
      children: [
        {
          path: '',
          name: 'admin',
          redirect: '/admin/user-manage/users'
        },
        {
          path: 'user-manage/users',
          name: 'admin-users',
          component: UsersView
        },
        {
          path: 'user-manage/roles',
          name: 'admin-roles',
          component: RolesView
        },
        {
          path: 'users',
          redirect: '/admin/user-manage/users'
        },
        {
          path: 'roles',
          redirect: '/admin/user-manage/roles'
        },
        {
          path: 'data',
          component: DataView,
          children: [
            {
              path: '',
              name: 'admin-data',
              redirect: '/admin/data/vehicles'
            },
            {
              path: 'vehicles',
              name: 'admin-data-vehicles',
              component: VehiclesView
            },
            {
              path: 'vehicle/:id',
              name: 'VehicleDetail',
              component: VehicleDetailView
            },
            {
              path: 'rcu-objects',
              name: 'admin-data-rcu-objects',
              component: RcuObjectsView
            }
          ]
        },
        {
          path: 'permissions',
          name: 'admin-permissions',
          component: PermissionsView
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: ProfileView
        },
        {
          path: 'system',
          component: SystemView,
          children: [
            {
              path: '',
              name: 'admin-system',
              redirect: '/admin/system/parameters'
            },
            {
              path: 'parameters',
              name: 'admin-system-parameters',
              component: ParametersView
            },
            {
              path: 'topics',
              name: 'admin-system-topics',
              component: TopicsView
            },
            {
              path: 'logs',
              name: 'admin-system-logs',
              component: LogsView
            }
          ]
        }
      ]
    }
  ]
})

export default router
