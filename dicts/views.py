from django.shortcuts import render
from django.views.generics import View
from django.views.decorators.csrf import csrf_exempt


class APIView(View):
    @csrf_exempt
    def dispatch(self, *args, **kwargs):
        return super().dispatch(*args, **kwargs)

    def post(self, request, *args, **kwargs):
        return HttpResponse('Hello world')
