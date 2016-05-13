from django.db import models
from django.conf import settings


class Dictionary(models.Model):
    name = models.CharField(max_length=1024)
    complete = models.BooleanField(default=False)
    structure = models.CharField(max_length=256)
    date_added = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name


class Article(models.Model):
    dictionary = models.ForeignKey(Dictionary)
    name = models.CharField(max_length=256)
    
    def __str__(self):
        return '{1} ({0})'.format(self.dictionary, self.name)  # TODO named params # TODO gettext


class NullUser(object):
    username = '(No user)'


class ArticleVersion(models.Model):
    article = models.ForeignKey(Article)
    body = models.TextField(blank=True)
    date_added = models.DateTimeField(auto_now_add=True)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, blank=True, null=True)
    ACTION_CHOICES = [
        (True, 'Add'),
        (False, 'Retract'),
    ]
    action = models.BooleanField(default=True, choices=ACTION_CHOICES)

    def __str__(self):
        return '{article} by {user} at {datetime}'.format(user=(self.user or NullUser).username, article=self.article.name, datetime=self.date_added)
