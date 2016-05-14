from django.contrib import admin
from . import models

admin.site.register(models.Dictionary)


from reversion.admin import VersionAdmin

class ArticleAdmin(VersionAdmin):
    pass

admin.site.register(models.Article, ArticleAdmin)
admin.site.register(models.Language)
