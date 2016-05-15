"""zarnidict_web URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/1.9/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  url(r'^$', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  url(r'^$', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.conf.urls import url, include
    2. Add a URL to urlpatterns:  url(r'^blog/', include('blog.urls'))
"""
from django.conf.urls import url, include
from django.contrib import admin
from dicts.views import APIView, OmUIView
from django.views.static import serve
from django.conf.urls.static import static
from django.conf import settings
from django.http import Http404


def serve_om_app(request, **kwargs):
    try:
        return serve(request, **kwargs)
    except Http404:
        return OmUIView.as_view()(request, **kwargs)

urlpatterns = [
    url(r'^api-view', APIView.as_view()),
    url(r'^admin/', admin.site.urls),
] + static('/', view=serve_om_app, document_root=settings.OM_STATIC_DIR)


if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
