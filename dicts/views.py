from django.shortcuts import render
from django.views.generic import View, TemplateView
from django.views.decorators.csrf import csrf_exempt
import json
from django.http import HttpResponse
from .models import Article, ArticleVersion, Dictionary


class OmUIView(TemplateView):
    template_name = 'om-ui.html'


class APIView(View):
    @csrf_exempt
    def dispatch(self, *args, **kwargs):
        return super().dispatch(*args, **kwargs)

    def post(self, request, *args, **kwargs):
        action = request.POST.get('action', None)
        params = json.loads(request.POST.body)
        #payload = request.POST.get('payload', '')

        data = {}
        status = 200

        try:
            if action == 'start-dict-upload':
                try:
                    lang_in = Language.objects.get(iso_code=params['lang_in'])
                    lang_out = Language.objects.get(iso_code=params['lang_out'])
                except Language.DoesNotExist:
                    data['error'] = 'Error: Make sure that languages exist: "{lang_in}", "{lang_out}"'.format(**params)
                    status = 400
                else:
                    params = json.loads(request.POST.get['params'])
                    dictionary = Dictionary.objects.create(
                        name=params['name'],
                        lang_in=lang_in,
                        lang_out=lang_out,
                        complete=False,
                        structure=params['structure']
                    )
                    data['dictionary'] = dictionary.id
            elif action == 'upload-articles':
                #articles['dict_id'] = json.loads(request.POST.get('articles'))
                #for article_data in articles['articles']:
                #    article_obj ....
                #    ArticleVersion.objects.create()
                pass
            else:
                pass
        except Exception as e:
            data['exception'] = str(e)
            status = 500

        return HttpResponse(json.dumps(data), status=status)
