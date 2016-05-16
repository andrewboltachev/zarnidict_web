from django.shortcuts import render
from django.views.generic import View, TemplateView
from django.views.decorators.csrf import csrf_exempt
import json, datetime, re, urllib
from django.http import HttpResponse
from .models import Article, ArticleVersion, Dictionary, NullUser
from django.core.paginator import Paginator
from functools import reduce

ITEMS_PER_PAGE = 20

class OmUIView(TemplateView):
    template_name = 'om-ui.html'

merge = lambda *d: dict(
        reduce(lambda items1, items2: list(items1) + list(items2), map(lambda x: x.items(), d))
        )

dict_vals = lambda f, d: {k: f(v) for k, v in d.items()}
model_to_dict = lambda x: dict(filter(lambda x: not x[0].startswith('_'), x.__dict__.items()))
model_to_dict2 = lambda x: dict_vals(
        lambda x: str(x) if isinstance(x, datetime.datetime) else x,
        model_to_dict(x)
        )


class APIView(View):
    @csrf_exempt
    def dispatch(self, *args, **kwargs):
        return super().dispatch(*args, **kwargs)

    def post(self, request, *args, **kwargs):
        full_url = request.POST.get('url', None)
        url_parsed = urllib.parse.urlparse(full_url)
        url = url_parsed.path
        url_params = dict_vals(
                lambda x: x[0],
                urllib.parse.parse_qs(url_parsed.query))
        action = request.POST.get('action', None)
        #params = json.loads(request.body)
        #payload = request.POST.get('payload', '')

        data = {'echo': request.POST}
        status = 200

        if url == '/':
            if action == 'get':
                data['list'] = list(map(model_to_dict2, Dictionary.objects.all()))

        match = re.match(r'^/(\d+)$', url)
        if match:
            page_no = int(url_params.get('page', '1'))
            id = int(match.groups()[0])
            data['name'] = Dictionary.objects.get(pk=id).name
            objects = Article.objects.filter(dictionary__pk=id)
            paginator = Paginator(objects, ITEMS_PER_PAGE)
            page = paginator.page(page_no)
            data['list'] = list(map(model_to_dict2, page.object_list))
            data['next_page_number'] = page.next_page_number() if page.has_next() else None
            data['previous_page_number'] = page.previous_page_number() if page.has_previous() else None
            page_range = paginator.page_range
            data['first_page_number'] = page_range.start if paginator.num_pages > 1 and page_range.start != page_no else None
            data['last_page_number'] = page_range.stop - 1 if paginator.num_pages > 1 and page_range.stop != page_no else None
            data['page_number'] = page_no
            data['url-path'] = url

        match = re.match(r'^/article/(\d+)$', url)
        if match:
            id = int(match.groups()[0])
            revision = url_params.get('revision', None)
            revision = None if revision is None else int(revision)
            print("revision", revision)

            article = Article.objects.get(pk=id)
            if action == "set":
                ArticleVersion.objects.create(article=article, body=request.POST['payload'], user=request.user if request.user.is_authenticated() else None)
                data['url_to_set'] = url
            article_version = article.articleversion_set.last() if revision is None else article.articleversion_set.all().get(pk=revision)
            data['list'] = list(
                    map(lambda x: merge(model_to_dict2(x), {'active': x == article_version, 'user': (x.user or NullUser).username, 'body': None}), article.articleversion_set.all())
                    )
            data['name'] = article.name
            data['id'] = article.id
            data['article'] = model_to_dict2(article_version)
            data['dictionary_id'] = article.dictionary.id

            for k, v in {'prev': '__lt', 'next': '__gt'}.items():
                articles = article.dictionary.article_set.filter(**{'id' + v: article.id})
                data[k] = model_to_dict2(articles[0]) if len(articles) else None

        """
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
        """

        return HttpResponse(json.dumps(data, ensure_ascii=False), status=status)
