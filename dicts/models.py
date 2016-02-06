from django.db import models


class Dictionary(models.Model):
    name = models.CharField(max_length=1024)

    def __str__(self):
        return self.name


class Article(models.Model):
    name = models.CharField(max_length=1024)
    body = models.TextField(blank=True)

    def __str__(self):
        return self.name
